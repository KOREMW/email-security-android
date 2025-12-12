package com.mop.phishing.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * Cloud Functions API Repository
 * 싱글톤 패턴으로 Retrofit 인스턴스 관리
 */
object CloudFunctionRepository {
    
    private const val TAG = "CloudFunctionRepo"
    
    // ============================================================
    // 🔧 설정: 환경에 따라 BASE_URL 변경
    // ============================================================
    
    // 에뮬레이터용 (로컬 개발)
    private const val EMULATOR_URL = "http://10.0.2.2:5001/email-security-phishing-1/us-central1/"
    
    // 실제 Firebase Functions (배포 후)
    // private const val PRODUCTION_URL = "https://us-central1-email-security-phishing-1.cloudfunctions.net/"
    
    // 현재 사용 URL
    private const val BASE_URL = EMULATOR_URL

    // ============================================================
    // Retrofit 설정
    // ============================================================
    
    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)  // AI 분석은 시간이 더 걸릴 수 있음
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CloudFunctionApi = retrofit.create(CloudFunctionApi::class.java)

    // ============================================================
    // API 메서드
    // ============================================================

    /**
     * 헬스 체크
     */
    suspend fun healthCheck(): HealthResponse {
        Log.d(TAG, "healthCheck 호출")
        return api.healthCheck()
    }

    /**
     * URL 악성 여부 분석
     * 
     * @param url 분석할 URL
     * @param source 요청 출처 (manual, notification, qr)
     * @param localRiskScore 로컬 ML 모델의 예측 점수 (0.0~1.0)
     */
    suspend fun analyzeSuspiciousUrl(
        url: String,
        source: String = "manual",
        localRiskScore: Double? = null
    ): AnalyzeSuspiciousUrlResponse {
        Log.d(TAG, "analyzeSuspiciousUrl 호출: url=$url, source=$source, localScore=$localRiskScore")
        
        val request = AnalyzeSuspiciousUrlRequest(
            url = url,
            source = source,
            localRiskScore = localRiskScore
        )
        
        return api.analyzeSuspiciousUrl(request)
    }

    /**
     * AI 상세 분석 보고서 요청
     * 
     * @param url 분석 대상 URL
     * @param analysisResponse 이전 analyzeSuspiciousUrl의 응답
     */
    suspend fun getAIAnalysis(
        url: String,
        analysisResponse: AnalyzeSuspiciousUrlResponse
    ): GetAIAnalysisResponse {
        Log.d(TAG, "getAIAnalysis 호출: url=$url")
        
        // 백엔드가 기대하는 형식으로 변환
        val request = GetAIAnalysisRequest(
            url = url,
            analysisResult = AIAnalysisInput(
                finalRiskScore = analysisResponse.finalRiskScore,
                suspiciousLevel = analysisResponse.suspiciousLevel,
                virusTotal = analysisResponse.virusTotal
            )
        )
        
        return api.getAIAnalysis(request)
    }

    /**
     * AI 상세 분석 보고서 요청 (개별 파라미터 버전)
     */
    suspend fun getAIAnalysis(
        url: String,
        finalRiskScore: Double,
        suspiciousLevel: String,
        virusTotal: VirusTotalResult?
    ): GetAIAnalysisResponse {
        Log.d(TAG, "getAIAnalysis 호출: url=$url, score=$finalRiskScore, level=$suspiciousLevel")
        
        val request = GetAIAnalysisRequest(
            url = url,
            analysisResult = AIAnalysisInput(
                finalRiskScore = finalRiskScore,
                suspiciousLevel = suspiciousLevel,
                virusTotal = virusTotal
            )
        )
        
        return api.getAIAnalysis(request)
    }
}
