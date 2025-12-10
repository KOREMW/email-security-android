package com.mop.phishing.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CloudFunctionApi {

    @GET("health")
    suspend fun health(): HealthResponse

    @POST("analyzeSuspiciousUrl")
    suspend fun analyzeSuspiciousUrl(
        @Body request: AnalyzeSuspiciousUrlRequest
    ): AnalyzeSuspiciousUrlResponse

    @POST("getAIAnalysis")
    suspend fun getAIAnalysis(
        @Body request: GetAIAnalysisRequest
    ): GetAIAnalysisResponse
}
