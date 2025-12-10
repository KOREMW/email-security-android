package com.mop.phishing.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object CloudFunctionRepository {
    // Firebase Functions 에뮬레이터
    private const val BASE_URL = "http://10.0.2.2:5001/email-security-phishing-1/us-central1/"


    // OkHttp 클라이언트 설정 (타임아웃)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: CloudFunctionApi = retrofit.create(CloudFunctionApi::class.java)

    suspend fun analyzeSuspiciousUrl(
        url: String,
        source: String = "manual",
        localRiskScore: Double? = null
    ): AnalyzeSuspiciousUrlResponse {
        val request = AnalyzeSuspiciousUrlRequest(url, source, localRiskScore)
        return api.analyzeSuspiciousUrl(request)
    }

    suspend fun getAIAnalysis(
        url: String,
        finalRiskScore: Double?,
        virusTotal: VirusTotalSummary?
    ): GetAIAnalysisResponse {
        val request = GetAIAnalysisRequest(url, finalRiskScore, virusTotal)
        return api.getAIAnalysis(request)
    }
}
