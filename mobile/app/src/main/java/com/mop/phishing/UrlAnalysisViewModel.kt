package com.mop.phishing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mop.phishing.data.local.NotificationRepository
import com.mop.phishing.data.remote.*
import com.mop.phishing.ml.UrlClassifier
import com.mop.phishing.ui.components.RiskLevel
import com.mop.phishing.ui.screens.detail.AnalysisDetails
import com.mop.phishing.ui.screens.detail.AnalysisResult
import com.mop.phishing.ui.screens.detail.AnalysisState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * URL 분석 ViewModel
 * 로컬 ML + 서버 API 연동 + 분석 결과 캐싱
 */
class UrlAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "UrlAnalysisVM"
    }

    // ============================================================
    // State
    // ============================================================

    // 분석 상태
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Loading)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 에러 메시지
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 서버 응답 원본 (디버깅/상세 정보용)
    private val _serverResponse = MutableStateFlow<AnalyzeSuspiciousUrlResponse?>(null)
    val serverResponse: StateFlow<AnalyzeSuspiciousUrlResponse?> = _serverResponse.asStateFlow()

    // AI 분석 응답
    private val _aiResponse = MutableStateFlow<GetAIAnalysisResponse?>(null)
    val aiResponse: StateFlow<GetAIAnalysisResponse?> = _aiResponse.asStateFlow()

    // 로컬 ML 점수
    private val _localScore = MutableStateFlow(0f)
    val localScore: StateFlow<Float> = _localScore.asStateFlow()

    // 캐시에서 로드되었는지 여부
    private val _isFromCache = MutableStateFlow(false)
    val isFromCache: StateFlow<Boolean> = _isFromCache.asStateFlow()

    // ML 분류기 (싱글톤 사용)
    private val classifier: UrlClassifier? = try {
        UrlClassifier.getInstance(application.applicationContext)
    } catch (e: Exception) {
        Log.e(TAG, "UrlClassifier 초기화 실패: ${e.message}")
        null
    }

    // 알림 Repository (캐싱용)
    private val notificationRepository = NotificationRepository.getInstance(application.applicationContext)

    // ============================================================
    // Public Methods
    // ============================================================

    /**
     * 알림 ID로 분석 실행 (캐시 확인 포함)
     * 이미 분석된 알림이면 캐시된 결과를 바로 반환
     */
    fun analyzeNotification(
        notificationId: String,
        url: String,
        source: String = "notification",
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _analysisState.value = AnalysisState.Loading
            _errorMessage.value = null
            _isFromCache.value = false

            try {
                // Step 1: 캐시 확인 (강제 새로고침이 아닌 경우)
                if (!forceRefresh) {
                    val cachedResult = notificationRepository.getAnalyzedResultById(notificationId)
                    if (cachedResult != null) {
                        Log.d(TAG, "캐시된 분석 결과 사용: id=$notificationId")
                        _isFromCache.value = true
                        
                        // 캐시된 결과를 AnalysisResult로 변환
                        val analysisResult = AnalysisResult(
                            riskLevel = RiskLevel.valueOf(cachedResult.riskLevel),
                            riskScore = cachedResult.riskScore,
                            recommendation = cachedResult.recommendation 
                                ?: getDefaultRecommendation(RiskLevel.valueOf(cachedResult.riskLevel)),
                            canAccess = cachedResult.canAccess,
                            details = AnalysisDetails(
                                localMlScore = cachedResult.localMlScore,
                                virusTotalMalicious = cachedResult.virusTotalMalicious,
                                virusTotalSuspicious = cachedResult.virusTotalSuspicious,
                                virusTotalTotal = cachedResult.virusTotalTotal,
                                finalScore = cachedResult.finalScore,
                                aiExplanation = cachedResult.aiExplanation
                            )
                        )
                        
                        _analysisState.value = AnalysisState.Completed(analysisResult)
                        _isLoading.value = false
                        return@launch
                    }
                }

                // Step 2: 새 분석 실행
                performAnalysis(url, source, notificationId)

            } catch (e: Exception) {
                Log.e(TAG, "분석 실패: ${e.message}", e)
                _errorMessage.value = e.message
                _analysisState.value = AnalysisState.Error(
                    e.message ?: "알 수 없는 오류가 발생했습니다."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * URL 전체 분석 실행 (수동 입력용)
     * 1. 로컬 ML 분석
     * 2. 서버 API 호출
     * 3. (선택) AI 상세 분석
     */
    fun analyzeUrl(
        url: String,
        source: String = "manual",
        includeAIAnalysis: Boolean = true,
        notificationId: String? = null
    ) {
        if (url.isBlank()) {
            _analysisState.value = AnalysisState.Error("URL을 입력하세요.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _analysisState.value = AnalysisState.Loading
            _errorMessage.value = null
            _isFromCache.value = false

            try {
                performAnalysis(url, source, notificationId, includeAIAnalysis)
            } catch (e: Exception) {
                Log.e(TAG, "분석 실패: ${e.message}", e)
                _errorMessage.value = e.message
                _analysisState.value = AnalysisState.Error(
                    e.message ?: "알 수 없는 오류가 발생했습니다."
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 실제 분석 수행 (공통 로직)
     */
    private suspend fun performAnalysis(
        url: String,
        source: String,
        notificationId: String?,
        includeAIAnalysis: Boolean = true
    ) {
        // Step 1: 로컬 ML 분석
        val localRiskScore = runLocalAnalysis(url)
        _localScore.value = localRiskScore
        Log.d(TAG, "로컬 ML 점수: $localRiskScore")

        // Step 2: 서버 API 호출
        val response = CloudFunctionRepository.analyzeSuspiciousUrl(
            url = url,
            source = source,
            localRiskScore = localRiskScore.toDouble()
        )
        _serverResponse.value = response
        Log.d(TAG, "서버 응답: suspicious=${response.suspicious}, score=${response.finalRiskScore}, level=${response.suspiciousLevel}")

        // Step 3: AI 분석 (모든 URL에 대해 요청)
        var aiAnalysisResponse: GetAIAnalysisResponse? = null
        if (includeAIAnalysis && shouldRequestAIAnalysis(response)) {
            try {
                aiAnalysisResponse = CloudFunctionRepository.getAIAnalysis(url, response)
                _aiResponse.value = aiAnalysisResponse
                Log.d(TAG, "AI 분석 완료: ${aiAnalysisResponse.riskSummary}")
            } catch (aiError: Exception) {
                Log.w(TAG, "AI 분석 실패 (무시): ${aiError.message}")
            }
        }

        // Step 4: UI 모델로 변환
        val analysisResult = ResponseMapper.mapToAnalysisResult(response, aiAnalysisResponse)
        _analysisState.value = AnalysisState.Completed(analysisResult)

        // Step 5: DB에 결과 저장 (notificationId가 있는 경우)
        if (notificationId != null) {
            saveAnalysisResultToDb(notificationId, analysisResult, response, aiAnalysisResponse)
        }
    }

    /**
     * 분석 결과를 DB에 저장
     */
    private suspend fun saveAnalysisResultToDb(
        notificationId: String,
        result: AnalysisResult,
        response: AnalyzeSuspiciousUrlResponse,
        aiResponse: GetAIAnalysisResponse?
    ) {
        try {
            // VirusTotal 전체 엔진 수 계산
            val vtTotal = response.breakdown?.virusTotalStats?.total
                ?: ((response.virusTotal?.malicious ?: 0) +
                    (response.virusTotal?.harmless ?: 0) +
                    (response.virusTotal?.undetected ?: 0) +
                    (response.virusTotal?.suspicious ?: 0))

            notificationRepository.updateAnalysisResult(
                id = notificationId,
                riskLevel = result.riskLevel,
                riskScore = result.riskScore,
                localMlScore = _localScore.value,
                vtMalicious = response.virusTotal?.malicious ?: 0,
                vtSuspicious = response.virusTotal?.suspicious ?: 0,
                vtTotal = vtTotal,
                finalScore = result.details?.finalScore ?: result.riskScore,
                aiExplanation = result.details?.aiExplanation,
                recommendation = result.recommendation,
                canAccess = result.canAccess
            )
            Log.d(TAG, "분석 결과 DB 저장 완료: id=$notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "분석 결과 DB 저장 실패: ${e.message}")
        }
    }

    /**
     * 로컬 ML 분석만 실행
     */
    fun runLocalAnalysisOnly(url: String): Float {
        val score = runLocalAnalysis(url)
        _localScore.value = score
        return score
    }

    /**
     * 분석 상태 초기화
     */
    fun resetAnalysis() {
        _analysisState.value = AnalysisState.Loading
        _serverResponse.value = null
        _aiResponse.value = null
        _localScore.value = 0f
        _errorMessage.value = null
        _isFromCache.value = false
    }

    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ============================================================
    // Private Methods
    // ============================================================

    /**
     * 로컬 TFLite 모델로 분석
     */
    private fun runLocalAnalysis(url: String): Float {
        return try {
            classifier?.predict(url) ?: 0.5f
        } catch (e: Exception) {
            Log.w(TAG, "로컬 분석 실패, 기본값 사용: ${e.message}")
            0.5f
        }
    }

    /**
     * AI 분석 요청 여부 결정
     * 모든 URL에 대해 AI 분석 요청 (사용자에게 상세 정보 제공)
     */
    private fun shouldRequestAIAnalysis(response: AnalyzeSuspiciousUrlResponse): Boolean {
        // 모든 URL에 대해 AI 분석 수행 (SAFE 포함)
        // 비용 절감이 필요하면 아래 조건으로 변경:
        // val level = response.suspiciousLevel.uppercase()
        // return response.suspicious || level in listOf("SUSPICIOUS", "DANGEROUS")
        return true
    }

    /**
     * 기본 권장 사항 반환
     */
    private fun getDefaultRecommendation(riskLevel: RiskLevel): String {
        return when (riskLevel) {
            RiskLevel.SAFE -> "안전한 링크입니다. 정상적으로 접속하셔도 됩니다."
            RiskLevel.CAUTION -> "주의가 필요한 링크입니다. 개인정보 입력에 주의하세요."
            RiskLevel.DANGER -> "위험한 링크입니다. 접속을 권장하지 않습니다."
            RiskLevel.UNKNOWN -> "분석 중입니다. 잠시만 기다려주세요."
        }
    }

    override fun onCleared() {
        super.onCleared()
        // 싱글톤이므로 여기서 close하지 않음
    }
}
