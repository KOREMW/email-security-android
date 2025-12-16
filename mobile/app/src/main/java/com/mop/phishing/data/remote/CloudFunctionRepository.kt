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
 * 
 * 백엔드의 모든 API 엔드포인트 지원:
 * - 핵심: analyzeSuspiciousUrl, getAIAnalysis
 * - 유틸리티: analyzeUrl, saveAnalysis, healthCheck
 * - 캐시: getCacheStats, cleanupCache
 * - 배치: batchAnalyzeUrls
 * - 로그: getErrorLogs
 */
object CloudFunctionRepository {
    
    private const val TAG = "CloudFunctionRepo"
    
    // ============================================================
    // 🔧 설정: 환경에 따라 BASE_URL 변경
    // ============================================================
    
    // 에뮬레이터용 (로컬 개발)
    // private const val EMULATOR_URL = "http://10.0.2.2:5001/email-security-phishing-1/us-central1/"
    
    // 실제 Firebase Functions (배포 후)
    private const val PRODUCTION_URL = "https://us-central1-email-security-phishing-1.cloudfunctions.net/"
    
    // 현재 사용 URL
    private const val BASE_URL = PRODUCTION_URL

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
    // 핵심 API 메서드
    // ============================================================

    /**
     * URL 악성 여부 분석
     *
     * @param url 분석할 URL
     * @param source 요청 출처 (manual, notification, qr)
     * @param localRiskScore 로컬 ML 모델의 예측 점수 (0.0~1.0)
     * @param forceRefresh 캐시 무시하고 강제 재분석 (기본 false)
     */
    suspend fun analyzeSuspiciousUrl(
        url: String,
        source: String = "manual",
        localRiskScore: Double? = null,
        forceRefresh: Boolean = false
    ): AnalyzeSuspiciousUrlResponse {
        Log.d(TAG, "analyzeSuspiciousUrl 호출: url=$url, source=$source, localScore=$localRiskScore, forceRefresh=$forceRefresh")

        val request = AnalyzeSuspiciousUrlRequest(
            url = url,
            source = source,
            localRiskScore = localRiskScore,
            forceRefresh = forceRefresh
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

    // ============================================================
    // 유틸리티 API 메서드
    // ============================================================

    /**
     * 간단한 URL 분석 (캐시 확인용)
     * 
     * @param url 분석할 URL
     * @param save 결과 저장 여부
     */
    suspend fun analyzeUrl(
        url: String,
        save: Boolean = false
    ): AnalyzeSuspiciousUrlResponse {
        Log.d(TAG, "analyzeUrl 호출: url=$url, save=$save")
        
        val request = AnalyzeUrlRequest(
            url = url,
            save = save
        )
        
        return api.analyzeUrl(request)
    }

    /**
     * 분석 결과 저장
     * 
     * @param url 분석한 URL
     * @param analysisResponse 저장할 분석 결과
     */
    suspend fun saveAnalysis(
        url: String,
        analysisResponse: AnalyzeSuspiciousUrlResponse
    ): SaveAnalysisResponse {
        Log.d(TAG, "saveAnalysis 호출: url=$url")
        
        val request = SaveAnalysisRequest(
            url = url,
            analysisData = AnalysisData(
                finalRiskScore = analysisResponse.finalRiskScore,
                suspiciousLevel = analysisResponse.suspiciousLevel,
                suspicious = analysisResponse.suspicious,
                confidence = analysisResponse.confidence,
                virusTotal = analysisResponse.virusTotal,
                breakdown = analysisResponse.breakdown,
                source = analysisResponse.source
            )
        )
        
        return api.saveAnalysis(request)
    }

    /**
     * 분석 결과 저장 (개별 파라미터 버전)
     */
    suspend fun saveAnalysis(
        url: String,
        finalRiskScore: Double,
        suspiciousLevel: String,
        suspicious: Boolean,
        confidence: Double? = null,
        virusTotal: VirusTotalResult? = null,
        breakdown: ScoreBreakdown? = null,
        source: String? = null
    ): SaveAnalysisResponse {
        Log.d(TAG, "saveAnalysis 호출: url=$url, score=$finalRiskScore")
        
        val request = SaveAnalysisRequest(
            url = url,
            analysisData = AnalysisData(
                finalRiskScore = finalRiskScore,
                suspiciousLevel = suspiciousLevel,
                suspicious = suspicious,
                confidence = confidence,
                virusTotal = virusTotal,
                breakdown = breakdown,
                source = source
            )
        )
        
        return api.saveAnalysis(request)
    }

    /**
     * 헬스 체크
     */
    suspend fun healthCheck(): HealthResponse {
        Log.d(TAG, "healthCheck 호출")
        return api.healthCheck()
    }

    // ============================================================
    // 캐시 관리 API 메서드
    // ============================================================

    /**
     * 캐시 통계 조회
     * 
     * @return 메모리 캐시 및 Firestore 통계
     */
    suspend fun getCacheStats(): CacheStatsResponse {
        Log.d(TAG, "getCacheStats 호출")
        return api.getCacheStats()
    }

    /**
     * 캐시 정리 (수동)
     * 
     * @param daysOld 며칠 이상 된 데이터 삭제 (기본 7일)
     * @return 정리 결과 (삭제된 항목 수)
     */
    suspend fun cleanupCache(daysOld: Int = 7): CleanupCacheResponse {
        Log.d(TAG, "cleanupCache 호출: daysOld=$daysOld")
        
        val request = CleanupCacheRequest(daysOld = daysOld)
        return api.cleanupCache(request)
    }

    // ============================================================
    // 배치 분석 API 메서드
    // ============================================================

    /**
     * 여러 URL 배치 분석 (최대 30개)
     * 
     * @param urls 분석할 URL 목록
     * @param localRiskScores 각 URL의 로컬 ML 점수 (선택, urls와 같은 순서)
     * @return 각 URL별 분석 결과
     */
    suspend fun batchAnalyzeUrls(
        urls: List<String>,
        localRiskScores: List<Double>? = null
    ): BatchAnalyzeUrlsResponse {
        Log.d(TAG, "batchAnalyzeUrls 호출: urls=${urls.size}개")
        
        if (urls.size > 30) {
            throw IllegalArgumentException("최대 30개 URL까지만 배치 분석 가능합니다.")
        }
        
        val request = BatchAnalyzeUrlsRequest(
            urls = urls,
            localRiskScores = localRiskScores
        )
        
        return api.batchAnalyzeUrls(request)
    }

    /**
     * 여러 URL 배치 분석 (로컬 점수 없이)
     */
    suspend fun batchAnalyzeUrls(urls: List<String>): BatchAnalyzeUrlsResponse {
        return batchAnalyzeUrls(urls, null)
    }

    // ============================================================
    // 로그 조회 API 메서드
    // ============================================================

    /**
     * 에러 로그 조회
     * 
     * @param limit 조회할 로그 수 (기본 50)
     * @param type 로그 타입 필터 ("all", "ERROR", "WARN")
     * @return 에러 로그 목록
     */
    suspend fun getErrorLogs(
        limit: Int = 50,
        type: String = "all"
    ): ErrorLogsResponse {
        Log.d(TAG, "getErrorLogs 호출: limit=$limit, type=$type")
        return api.getErrorLogs(limit, type)
    }

    /**
     * 에러 로그만 조회
     */
    suspend fun getErrorLogsOnly(limit: Int = 50): ErrorLogsResponse {
        return getErrorLogs(limit, "ERROR")
    }

    /**
     * 경고 로그만 조회
     */
    suspend fun getWarningLogsOnly(limit: Int = 50): ErrorLogsResponse {
        return getErrorLogs(limit, "WARN")
    }
}
