package com.example.phishing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.example.phishing.data.remote.CloudFunctionRepository
import com.example.phishing.data.remote.AnalyzeSuspiciousUrlResponse
import android.util.Log

class UrlAnalysisViewModel : ViewModel() {

    private val TAG = "UrlAnalysisViewModel"

    // 서버 응답 결과
    private val _serverResponse = MutableLiveData<AnalyzeSuspiciousUrlResponse?>()
    val serverResponse: LiveData<AnalyzeSuspiciousUrlResponse?> = _serverResponse

    // 로컬 TFLite 점수
    private val _localScore = MutableLiveData<Float>(0f)
    val localScore: LiveData<Float> = _localScore

    // 상태 메시지 (에러 등)
    private val _statusMessage = MutableLiveData<String>("")
    val statusMessage: LiveData<String> = _statusMessage

    // 로딩 상태
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * 로컬 TFLite 점수 설정
     */
    fun setLocalScore(score: Float) {
        _localScore.value = score
    }

    /**
     * URL 서버 분석 (로컬 점수 함께 전송)
     */
    fun analyzeUrlWithServer(url: String, localRiskScore: Float? = null) {
        if (url.isBlank()) {
            _statusMessage.value = "URL을 입력하세요."
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val response = CloudFunctionRepository.analyzeSuspiciousUrl(
                    url = url,
                    source = "manual",
                    localRiskScore = localRiskScore?.toDouble()
                )

                _serverResponse.value = response
                _statusMessage.value = "서버 분석 완료"

                Log.d(TAG, "Server response: $response")

            } catch (e: Exception) {
                _statusMessage.value = "서버 오류: ${e.message}"
                Log.e(TAG, "Error analyzing URL", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
