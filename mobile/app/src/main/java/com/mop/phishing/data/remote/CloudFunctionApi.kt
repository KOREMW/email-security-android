package com.mop.phishing.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Firebase Cloud Functions API 인터페이스
 * 백엔드 index.js의 엔드포인트와 일치
 */
interface CloudFunctionApi {

    /**
     * 헬스 체크
     * GET /healthCheck
     */
    @GET("healthCheck")
    suspend fun healthCheck(): HealthResponse

    /**
     * URL 악성 여부 분석 (핵심 API)
     * POST /analyzeSuspiciousUrl
     * 
     * @param request URL, source, localRiskScore 포함
     * @return 분석 결과 (suspicious, finalRiskScore, virusTotal 등)
     */
    @POST("analyzeSuspiciousUrl")
    suspend fun analyzeSuspiciousUrl(
        @Body request: AnalyzeSuspiciousUrlRequest
    ): AnalyzeSuspiciousUrlResponse

    /**
     * AI 기반 상세 분석 보고서 생성
     * POST /getAIAnalysis
     * 
     * @param request url + analysisResult 객체
     * @return AI 생성 보고서 (riskSummary, riskFactors, recommendations)
     */
    @POST("getAIAnalysis")
    suspend fun getAIAnalysis(
        @Body request: GetAIAnalysisRequest
    ): GetAIAnalysisResponse

    /**
     * 간단한 URL 분석 (캐시 확인용)
     * POST /analyzeUrl
     */
    @POST("analyzeUrl")
    suspend fun analyzeUrl(
        @Body request: AnalyzeUrlRequest
    ): AnalyzeSuspiciousUrlResponse
}

/**
 * /analyzeUrl 요청
 */
data class AnalyzeUrlRequest(
    val url: String,
    val save: Boolean = false
)
