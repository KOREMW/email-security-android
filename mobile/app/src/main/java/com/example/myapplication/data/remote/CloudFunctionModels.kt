package com.example.myapplication.data.remote

import com.google.gson.annotations.SerializedName

// ============ Request/Response for /analyzeSuspiciousUrl ============

data class AnalyzeSuspiciousUrlRequest(
    val url: String,
    val source: String = "manual",  // manual | notification | qr
    @SerializedName("localRiskScore")
    val localRiskScore: Double? = null,
)

data class AnalyzeSuspiciousUrlResponse(
    val suspicious: Boolean,
    @SerializedName("finalRiskScore")
    val finalRiskScore: Double,
    val virusTotal: VirusTotalSummary?,
    val redirects: List<String>,
    val error: String?,
    val debug: DebugInfo?,
)

data class VirusTotalSummary(
    val malicious: Int,
    val harmless: Int,
    val undetected: Int,
)

data class DebugInfo(
    val source: String?,
)

// ============ Request/Response for /getAIAnalysis ============

data class GetAIAnalysisRequest(
    val url: String,
    @SerializedName("finalRiskScore")
    val finalRiskScore: Double?,
    val virusTotal: VirusTotalSummary?,
)

data class GetAIAnalysisResponse(
    val summary: String,
    val reasons: List<String>,
    val recommendation: String,
    val echo: EchoData?,
    val error: String?,
)

data class EchoData(
    val url: String?,
    @SerializedName("finalRiskScore")
    val finalRiskScore: Double?,
    val virusTotal: VirusTotalSummary?,
)

// ============ Health Check Response ============

data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: String,
)
