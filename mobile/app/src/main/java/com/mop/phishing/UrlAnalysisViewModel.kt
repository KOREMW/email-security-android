package com.mop.phishing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mop.phishing.data.remote.*
import com.mop.phishing.ml.UrlClassifier
import com.mop.phishing.ui.components.RiskLevel
import com.mop.phishing.ui.screens.detail.AnalysisResult
import com.mop.phishing.ui.screens.detail.AnalysisState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * URL 분석 ViewModel
 * 로컬 ML + 서버 API 연동
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

    // ML 분류기
    private var classifier: UrlClassifier? = null

    init {
        try {
            classifier = UrlClassifier(application.applicationContext)
            Log.d(TAG, "UrlClassifier 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "UrlClassifier 초기화 실패: ${e.message}")
        }
    }

    // ============================================================
    // Public Methods
    // ============================================================

    /**
     * URL 전체 분석 실행
     * 1. 로컬 ML 분석
     * 2. 서버 API 호출
     * 3. (선택) AI 상세 분석
     */
    fun analyzeUrl(
        url: String,
        source: String = "manual",
        includeAIAnalysis: Boolean = true
    ) {
        if (url.isBlank()) {
            _analysisState.value = AnalysisState.Error("URL을 입력하세요.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _analysisState.value = AnalysisState.Loading
            _errorMessage.value = null

            try {
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

                // Step 3: AI 분석 (위험 레벨이 높거나 요청한 경우)
                var aiAnalysisResponse: GetAIAnalysisResponse? = null
                if (includeAIAnalysis && shouldRequestAIAnalysis(response)) {
                    try {
                        aiAnalysisResponse = CloudFunctionRepository.getAIAnalysis(url, response)
                        _aiResponse.value = aiAnalysisResponse
                        Log.d(TAG, "AI 분석 완료: ${aiAnalysisResponse.riskSummary}")
                    } catch (aiError: Exception) {
                        Log.w(TAG, "AI 분석 실패 (무시): ${aiError.message}")
                        // AI 분석 실패는 전체 실패로 처리하지 않음
                    }
                }

                // Step 4: UI 모델로 변환
                val analysisResult = ResponseMapper.mapToAnalysisResult(response, aiAnalysisResponse)
                _analysisState.value = AnalysisState.Completed(analysisResult)

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
     * - 위험 레벨이 MEDIUM 이상이면 요청
     * - 또는 suspicious가 true이면 요청
     */
    private fun shouldRequestAIAnalysis(response: AnalyzeSuspiciousUrlResponse): Boolean {
        val level = response.suspiciousLevel.uppercase()
        return response.suspicious || 
               level in listOf("MEDIUM", "SUSPICIOUS", "DANGEROUS")
    }

    override fun onCleared() {
        super.onCleared()
        classifier?.close()
        classifier = null
    }
}
