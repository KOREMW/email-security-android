package com.mop.phishing.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Firebase Cloud Functions API 인터페이스
 * 백엔드 index.js의 모든 엔드포인트와 일치
 * 
 * 총 10개 API:
 * - 핵심: analyzeSuspiciousUrl, getAIAnalysis
 * - 유틸리티: analyzeUrl, saveAnalysis, healthCheck
 * - 캐시: getCacheStats, cleanupCache
 * - 배치: batchAnalyzeUrls
 * - 로그: getErrorLogs
 */
interface CloudFunctionApi {

    // ============================================================
    // 핵심 API
    // ============================================================

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

    // ============================================================
    // 유틸리티 API
    // ============================================================

    /**
     * 간단한 URL 분석 (캐시 확인용)
     * POST /analyzeUrl
     */
    @POST("analyzeUrl")
    suspend fun analyzeUrl(
        @Body request: AnalyzeUrlRequest
    ): AnalyzeSuspiciousUrlResponse

    /**
     * 분석 결과 저장
     * POST /saveAnalysis
     * 
     * @param request url + analysisData 객체
     * @return 저장 결과
     */
    @POST("saveAnalysis")
    suspend fun saveAnalysis(
        @Body request: SaveAnalysisRequest
    ): SaveAnalysisResponse

    /**
     * 헬스 체크
     * GET /healthCheck
     */
    @GET("healthCheck")
    suspend fun healthCheck(): HealthResponse

    // ============================================================
    // 캐시 관리 API
    // ============================================================

    /**
     * 캐시 통계 조회
     * GET /getCacheStats
     * 
     * @return 메모리 캐시 + Firestore 통계
     */
    @GET("getCacheStats")
    suspend fun getCacheStats(): CacheStatsResponse

    /**
     * 캐시 정리 (수동)
     * POST /cleanupCache
     * 
     * @param request daysOld (며칠 이상 된 데이터 삭제, 기본 7일)
     * @return 정리 결과
     */
    @POST("cleanupCache")
    suspend fun cleanupCache(
        @Body request: CleanupCacheRequest
    ): CleanupCacheResponse

    // ============================================================
    // 배치 분석 API
    // ============================================================

    /**
     * 여러 URL 배치 분석 (최대 30개)
     * POST /batchAnalyzeUrls
     * 
     * @param request urls 배열 + localRiskScores 배열 (선택)
     * @return 각 URL별 분석 결과
     */
    @POST("batchAnalyzeUrls")
    suspend fun batchAnalyzeUrls(
        @Body request: BatchAnalyzeUrlsRequest
    ): BatchAnalyzeUrlsResponse

    // ============================================================
    // 로그 조회 API
    // ============================================================

    /**
     * 에러 로그 조회
     * GET /getErrorLogs
     * 
     * @param limit 조회할 로그 수 (기본 50)
     * @param type 로그 타입 필터 (all, ERROR, WARN)
     * @return 에러 로그 목록
     */
    @GET("getErrorLogs")
    suspend fun getErrorLogs(
        @Query("limit") limit: Int = 50,
        @Query("type") type: String = "all"
    ): ErrorLogsResponse
}

// ============================================================
// 요청 모델
// ============================================================

/**
 * /analyzeUrl 요청
 */
data class AnalyzeUrlRequest(
    val url: String,
    val save: Boolean = false
)

/**
 * /saveAnalysis 요청
 */
data class SaveAnalysisRequest(
    val url: String,
    val analysisData: AnalysisData
)

/**
 * 저장할 분석 데이터
 */
data class AnalysisData(
    val finalRiskScore: Double,
    val suspiciousLevel: String,
    val suspicious: Boolean,
    val confidence: Double?,
    val virusTotal: VirusTotalResult?,
    val breakdown: ScoreBreakdown?,
    val source: String? = null
)

/**
 * /cleanupCache 요청
 */
data class CleanupCacheRequest(
    val daysOld: Int = 7
)

/**
 * /batchAnalyzeUrls 요청
 */
data class BatchAnalyzeUrlsRequest(
    val urls: List<String>,
    val localRiskScores: List<Double>? = null
)

// ============================================================
// 응답 모델
// ============================================================

/**
 * /saveAnalysis 응답
 */
data class SaveAnalysisResponse(
    val message: String?,
    val url: String?,
    val timestamp: String?
)

/**
 * /getCacheStats 응답
 */
data class CacheStatsResponse(
    val memoryCache: MemoryCacheStats?,
    val firestoreStats: FirestoreStats?,
    val timestamp: String?
)

data class MemoryCacheStats(
    val hits: Int?,
    val misses: Int?,
    val hitRate: String?,
    val evictions: Int?,
    val size: Int?,
    val maxSize: Int?,
    val autoCleanup: Boolean?
)

data class FirestoreStats(
    val write: WriteStats?,
    val read: ReadStats?
)

data class WriteStats(
    val total: Int?,
    val success: Int?,
    val failed: Int?,
    val successRate: String?
)

data class ReadStats(
    val total: Int?,
    val success: Int?,
    val failed: Int?,
    val cacheHit: Int?,
    val hitRate: String?
)

/**
 * /cleanupCache 응답
 */
data class CleanupCacheResponse(
    val message: String?,
    val results: CleanupResults?,
    val cacheStats: MemoryCacheStats?,
    val timestamp: String?
)

data class CleanupResults(
    val memoryCache: Int?,
    val firestoreAnalysis: Int?,
    val oldLogs: Int?
)

/**
 * /batchAnalyzeUrls 응답
 */
data class BatchAnalyzeUrlsResponse(
    val message: String?,
    val total: Int?,
    val success: Int?,
    val failed: Int?,
    val processingTime: Long?,
    val results: List<BatchAnalysisResult>?,
    val timestamp: String?
)

data class BatchAnalysisResult(
    val url: String,
    val suspicious: Boolean?,
    val finalRiskScore: Double?,
    val suspiciousLevel: String?,
    val virusTotal: VirusTotalResult?,
    val status: String?,  // "success" | "error"
    val error: String?
)

/**
 * /getErrorLogs 응답
 */
data class ErrorLogsResponse(
    val logs: List<ErrorLogItem>?,
    val count: Int?,
    val timestamp: String?
)

data class ErrorLogItem(
    val id: String?,
    val type: String?,          // "ERROR" | "WARN" | "INFO"
    val endpoint: String?,
    val error: String?,
    val stack: String?,
    val duration: Long?,
    val timestamp: String?,
    val logId: String?,
    val createdAt: String?
)
