package com.mop.phishing.data.remote

import com.google.gson.annotations.SerializedName

// ============================================================
// /analyzeSuspiciousUrl API
// ============================================================

/**
 * 요청: POST /analyzeSuspiciousUrl
 */
data class AnalyzeSuspiciousUrlRequest(
    val url: String,
    val source: String = "manual",  // manual | notification | qr
    @SerializedName("localRiskScore")
    val localRiskScore: Double? = null
)

/**
 * 응답: POST /analyzeSuspiciousUrl
 * 백엔드 index.js의 analysisResult 구조와 일치
 */
data class AnalyzeSuspiciousUrlResponse(
    // 기본 URL 정보
    val url: String,
    val originalUrl: String?,
    val normalizedUrl: String?,
    
    // 분석 결과
    val suspicious: Boolean,
    @SerializedName("finalRiskScore")
    val finalRiskScore: Double,
    val suspiciousLevel: String,  // "SAFE", "SUSPICIOUS", "DANGEROUS"
    val confidence: Double?,
    
    // VirusTotal 결과
    val virusTotal: VirusTotalResult?,
    
    // 점수 상세 내역
    val breakdown: ScoreBreakdown?,
    
    // URL 특성 (urlAnalyzer.js의 features)
    val urlFeatures: UrlFeatures?,
    
    // 메타 정보
    val source: String?,
    val cached: Boolean?,
    val cacheSource: String?,  // "memory" | "firestore"
    val analyzedAt: String?,
    val processingTime: Long?,
    val timestamp: String?,
    
    // 에러 (있을 경우)
    val error: String?
)

/**
 * VirusTotal 분석 결과
 * 백엔드의 virusTotalResult 구조와 일치
 */
data class VirusTotalResult(
    val malicious: Int = 0,
    val harmless: Int = 0,
    val undetected: Int = 0,
    val suspicious: Int = 0,
    val status: String? = null,  // "skipped", "cached", "analyzed", "error"
    val errorMessage: String? = null
)

/**
 * 점수 계산 상세 내역
 * 백엔드 scoreCalculator.js의 breakdown 구조
 */
data class ScoreBreakdown(
    val localMlScore: Double?,
    val virusTotalScore: Double?,
    val weights: ScoreWeights?,
    val virusTotalStats: VirusTotalStats?
)

data class ScoreWeights(
    val localMl: Double?,
    val virusTotal: Double?
)

data class VirusTotalStats(
    val malicious: Int?,
    val harmless: Int?,
    val undetected: Int?,
    val total: Int?
)

/**
 * URL 특성 분석 결과
 * 백엔드 urlAnalyzer.js의 extractUrlFeatures() 반환 구조와 일치
 * 
 * 주의: hasHttps, hasPort 등은 Boolean이 아니라 Int (0 또는 1)
 */
data class UrlFeatures(
    // 길이 피처
    val urlLength: Int?,
    val domainLength: Int?,
    val pathLength: Int?,
    
    // 구조 피처
    val subdomainCount: Int?,
    val hasHttps: Int?,      // 0 또는 1 (Boolean 아님!)
    val hasPort: Int?,       // 0 또는 1
    val paramCount: Int?,
    
    // 문자 분석
    val digitRatio: Double?,
    val specialCharRatio: Double?,
    val hasIpAddress: Int?,  // 0 또는 1
    
    // 의심 패턴
    val hasSuspiciousKeyword: Int?,  // 0 또는 1
    val hasSuspiciousTld: Int?,      // 0 또는 1
    
    // 엔트로피 (랜덤성)
    val entropy: Double?
)

// ============================================================
// /getAIAnalysis API
// ============================================================

/**
 * 요청: POST /getAIAnalysis
 * 백엔드가 기대하는 구조: { url, analysisResult }
 */
data class GetAIAnalysisRequest(
    val url: String,
    val analysisResult: AIAnalysisInput
)

/**
 * getAIAnalysis 요청의 analysisResult 필드
 */
data class AIAnalysisInput(
    @SerializedName("finalRiskScore")
    val finalRiskScore: Double?,
    val suspiciousLevel: String?,
    val virusTotal: VirusTotalResult?
)

/**
 * 응답: POST /getAIAnalysis (상세 분석 버전)
 * 백엔드 응답 구조와 일치
 */
data class GetAIAnalysisResponse(
    val url: String?,
    val aiInsight: String?,
    val riskSummary: String?,

    // 🆕 상세 피싱 분석 필드
    val phishingType: String?,              // 피싱 유형 (예: "계정탈취", "금융사칭")
    val phishingTechniques: List<String>?,  // 사용된 피싱 기법
    val targetedBrand: String?,             // 사칭 대상 브랜드 (예: "네이버", "카카오")
    val technicalAnalysis: String?,         // 기술적 분석
    val attackScenario: String?,            // 예상 공격 시나리오

    val riskFactors: List<String>?,
    val recommendations: List<String>?,
    val analysisScore: Double?,
    val suspiciousLevel: String?,
    val processingTime: Long?,
    val generatedAt: String?,
    val error: String?
)

// ============================================================
// /healthCheck API
// ============================================================

/**
 * 응답: GET /healthCheck
 */
data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: String,
    val services: ServicesStatus?,
    val environment: EnvironmentInfo?,
    val endpoints: EndpointsInfo?
)

data class ServicesStatus(
    val firestore: FirestoreStatus?,
    val memoryCache: MemoryCacheStatus?,
    val virusTotal: ApiKeyStatus?,
    val openai: ApiKeyStatus?
)

data class FirestoreStatus(
    val status: String?
)

data class MemoryCacheStatus(
    val status: String?,
    val stats: CacheStats?
)

data class CacheStats(
    val size: Int?,
    val hits: Int?,
    val misses: Int?,
    val hitRate: Double?
)

data class ApiKeyStatus(
    val status: String?,  // "configured" | "not_configured"
    val keyMasked: String?
)

data class EnvironmentInfo(
    val region: String?,
    val nodeVersion: String?
)

data class EndpointsInfo(
    val core: List<String>?,
    val utility: List<String>?
)

// ============================================================
// 기타 유틸리티
// ============================================================

/**
 * 공통 에러 응답
 */
data class ErrorResponse(
    val error: String,
    val message: String?,
    val timestamp: String?
)
