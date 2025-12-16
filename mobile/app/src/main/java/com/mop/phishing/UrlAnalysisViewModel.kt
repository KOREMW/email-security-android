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
        
        // 🚨 빌드 확인용 - 이 값이 Logcat에 보이면 새 APK가 설치된 것
        private const val BUILD_ID = "2025-12-16-FIX-V3"
        
        /**
         * 🆕 URL 정규화 - 프로토콜이 없으면 추가
         * QR 코드에서 "naver.com"만 스캔될 때 "https://naver.com"으로 변환
         */
        fun normalizeUrl(url: String): String {
            val trimmed = url.trim()
            
            // 이미 프로토콜이 있으면 그대로 반환
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed
            }
            
            // 프로토콜이 없으면 https:// 추가
            return "https://$trimmed"
        }
    }

    // ============================================================
    // State
    // ============================================================

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Loading)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _serverResponse = MutableStateFlow<AnalyzeSuspiciousUrlResponse?>(null)
    val serverResponse: StateFlow<AnalyzeSuspiciousUrlResponse?> = _serverResponse.asStateFlow()

    private val _aiResponse = MutableStateFlow<GetAIAnalysisResponse?>(null)
    val aiResponse: StateFlow<GetAIAnalysisResponse?> = _aiResponse.asStateFlow()

    private val _localScore = MutableStateFlow(0f)
    val localScore: StateFlow<Float> = _localScore.asStateFlow()

    private val _isFromCache = MutableStateFlow(false)
    val isFromCache: StateFlow<Boolean> = _isFromCache.asStateFlow()

    // 🆕 현재 분석 중인 URL 저장 (재분석용)
    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val classifier: UrlClassifier? = try {
        Log.w(TAG, "████████████████████████████████████████")
        Log.w(TAG, "██ BUILD_ID: $BUILD_ID ██")
        Log.w(TAG, "████████████████████████████████████████")
        Log.d(TAG, "UrlClassifier 초기화 시작...")
        val instance = UrlClassifier.getInstance(application.applicationContext)
        Log.d(TAG, "✅ UrlClassifier 초기화 성공! VERSION=${UrlClassifier.VERSION}")
        instance
    } catch (e: Exception) {
        Log.e(TAG, "❌ UrlClassifier 초기화 실패: ${e.message}", e)
        null
    }

    private val notificationRepository = NotificationRepository.getInstance(application.applicationContext)

    // ============================================================
    // Public Methods
    // ============================================================

    /**
     * 알림 ID로 분석 실행 (캐시 확인 포함)
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
                } else {
                    Log.d(TAG, "🔄 전체 재분석 시작: id=$notificationId, url=$url")
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
     * URL 전체 분석 실행 (수동 입력 / QR 스캔용)
     *
     * @param forceRefresh true이면 서버 캐시를 무시하고 강제 재분석
     */
    fun analyzeUrl(
        url: String,
        source: String = "manual",
        includeAIAnalysis: Boolean = true,
        notificationId: String? = null,
        forceRefresh: Boolean = false
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
                performAnalysis(url, source, notificationId, includeAIAnalysis, forceRefresh)
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
        includeAIAnalysis: Boolean = true,
        forceRefresh: Boolean = false
    ) {
        // 🆕 Step 0: URL 정규화 (QR 코드에서 프로토콜 없이 스캔될 때 대응)
        val normalizedUrl = normalizeUrl(url)
        if (normalizedUrl != url) {
            Log.d(TAG, "🔗 URL 정규화: $url → $normalizedUrl")
        }

        // 🆕 현재 분석 URL 저장 (재분석용)
        _currentUrl.value = normalizedUrl

        // Step 1: 로컬 ML 분석
        Log.d(TAG, "🧠 온디바이스 ML 분석 시작: $normalizedUrl")
        val localRiskScore = runLocalAnalysis(normalizedUrl)
        _localScore.value = localRiskScore
        Log.d(TAG, "🧠 온디바이스 ML 분석 완료: 점수=$localRiskScore (${(localRiskScore * 100).toInt()}%)")

        // Step 2: 서버 API 호출 (VirusTotal 포함) - 🆕 정규화된 URL 사용
        Log.d(TAG, "🌐 서버 API 호출 시작 (VirusTotal 포함), forceRefresh=$forceRefresh")
        val response = CloudFunctionRepository.analyzeSuspiciousUrl(
            url = normalizedUrl,  // 🔧 정규화된 URL 전달
            source = source,
            localRiskScore = localRiskScore.toDouble(),
            forceRefresh = forceRefresh  // 🆕 캐시 무시 옵션
        )
        _serverResponse.value = response
        Log.d(TAG, "🌐 서버 응답: suspicious=${response.suspicious}, score=${response.finalRiskScore}, level=${response.suspiciousLevel}")
        Log.d(TAG, "🛡️ VirusTotal: malicious=${response.virusTotal?.malicious}, suspicious=${response.virusTotal?.suspicious}, total=${response.breakdown?.virusTotalStats?.total}")

        // Step 3: AI 분석
        var aiAnalysisResponse: GetAIAnalysisResponse? = null
        if (includeAIAnalysis && shouldRequestAIAnalysis(response)) {
            try {
                Log.d(TAG, "🤖 AI 분석 시작")
                aiAnalysisResponse = CloudFunctionRepository.getAIAnalysis(normalizedUrl, response)
                _aiResponse.value = aiAnalysisResponse
                Log.d(TAG, "🤖 AI 분석 완료: ${aiAnalysisResponse.riskSummary}")
            } catch (aiError: Exception) {
                Log.w(TAG, "🤖 AI 분석 실패 (무시): ${aiError.message}")
            }
        }

        // Step 4: UI 모델로 변환 (실제 로컬 ML 점수 전달)
        val analysisResult = ResponseMapper.mapToAnalysisResult(response, aiAnalysisResponse, localRiskScore)
        _analysisState.value = AnalysisState.Completed(analysisResult)
        Log.d(TAG, "✅ 분석 완료: riskLevel=${analysisResult.riskLevel}, riskScore=${analysisResult.riskScore}, localMlScore=${localRiskScore}")

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
        val score = runLocalAnalysis(normalizeUrl(url))
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

    private fun runLocalAnalysis(url: String): Float {
        Log.w(TAG, "══════════ runLocalAnalysis 시작 (BUILD: $BUILD_ID) ══════════")
        Log.w(TAG, "│ URL: $url")
        Log.w(TAG, "│ classifier: ${if (classifier == null) "❌ NULL" else "✅ OK (${UrlClassifier.VERSION})"}")

        if (classifier == null) {
            Log.e(TAG, "│ ❌ classifier가 null! 기본값 0.5 반환")
            Log.w(TAG, "══════════ runLocalAnalysis 종료 (FAILED) ══════════")
            return 0.5f
        }

        return try {
            val score = classifier.predict(url)
            Log.w(TAG, "│ ✅ predict() 성공!")
            Log.w(TAG, "│ ★ ML 점수: $score (${(score * 100).toInt()}%)")
            Log.w(TAG, "══════════ runLocalAnalysis 종료 (SUCCESS) ══════════")
            score
        } catch (e: Exception) {
            Log.e(TAG, "│ ❌ predict() 실패: ${e.message}", e)
            Log.w(TAG, "══════════ runLocalAnalysis 종료 (EXCEPTION) ══════════")
            0.5f
        }
    }

    private fun shouldRequestAIAnalysis(response: AnalyzeSuspiciousUrlResponse): Boolean {
        return true  // 모든 URL에 대해 AI 분석 수행
    }

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
    }
}
