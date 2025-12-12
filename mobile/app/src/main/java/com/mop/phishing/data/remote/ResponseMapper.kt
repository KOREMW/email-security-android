package com.mop.phishing.data.remote

import com.mop.phishing.ui.components.RiskLevel
import com.mop.phishing.ui.screens.detail.AnalysisDetails
import com.mop.phishing.ui.screens.detail.AnalysisResult

/**
 * API 응답 → UI 모델 변환기
 * 백엔드 응답을 UI에서 사용하는 형태로 변환
 */
object ResponseMapper {

    /**
     * 백엔드의 suspiciousLevel → UI의 RiskLevel 변환
     * 
     * 백엔드 값 (scoreCalculator.js):
     * - "SAFE": score < 0.3
     * - "SUSPICIOUS": 0.3 ≤ score < 0.7
     * - "DANGEROUS": score ≥ 0.7
     */
    fun mapSuspiciousLevelToRiskLevel(suspiciousLevel: String?): RiskLevel {
        return when (suspiciousLevel?.uppercase()) {
            "SAFE" -> RiskLevel.SAFE
            "SUSPICIOUS" -> RiskLevel.CAUTION
            "DANGEROUS" -> RiskLevel.DANGER
            else -> RiskLevel.UNKNOWN
        }
    }

    /**
     * finalRiskScore (0.0~1.0) → UI 점수 (0~100) 변환
     */
    fun mapScoreToPercent(score: Double?): Int {
        if (score == null) return 0
        return (score * 100).toInt().coerceIn(0, 100)
    }

    /**
     * RiskLevel에 따른 권장 사항 생성
     */
    fun getRecommendation(riskLevel: RiskLevel, aiRecommendations: List<String>?): String {
        // AI 권장사항이 있으면 사용
        if (!aiRecommendations.isNullOrEmpty()) {
            return aiRecommendations.joinToString("\n• ", prefix = "• ")
        }
        
        // 기본 권장사항
        return when (riskLevel) {
            RiskLevel.SAFE -> "안전한 링크입니다. 정상적으로 접속하셔도 됩니다."
            RiskLevel.CAUTION -> "주의가 필요한 링크입니다. 개인정보 입력에 주의하세요."
            RiskLevel.DANGER -> "위험한 링크입니다. 접속을 권장하지 않습니다."
            RiskLevel.UNKNOWN -> "분석 중입니다. 잠시만 기다려주세요."
        }
    }

    /**
     * RiskLevel에 따른 접속 가능 여부 판단
     */
    fun canAccess(riskLevel: RiskLevel): Boolean {
        return riskLevel == RiskLevel.SAFE
    }

    /**
     * API 응답 → UI AnalysisResult 변환
     */
    fun mapToAnalysisResult(
        response: AnalyzeSuspiciousUrlResponse,
        aiResponse: GetAIAnalysisResponse? = null
    ): AnalysisResult {
        val riskLevel = mapSuspiciousLevelToRiskLevel(response.suspiciousLevel)
        val riskScore = mapScoreToPercent(response.finalRiskScore)
        
        return AnalysisResult(
            riskLevel = riskLevel,
            riskScore = riskScore,
            recommendation = getRecommendation(riskLevel, aiResponse?.recommendations),
            canAccess = canAccess(riskLevel),
            details = mapToAnalysisDetails(response, aiResponse)
        )
    }

    /**
     * API 응답 → UI AnalysisDetails 변환
     */
    fun mapToAnalysisDetails(
        response: AnalyzeSuspiciousUrlResponse,
        aiResponse: GetAIAnalysisResponse? = null
    ): AnalysisDetails {
        return AnalysisDetails(
            localMlScore = response.breakdown?.localMlScore?.toFloat() ?: 0f,
            virusTotalMalicious = response.virusTotal?.malicious ?: 0,
            virusTotalSuspicious = response.virusTotal?.suspicious ?: 0,
            redirectCount = 0,  // 백엔드에서 redirect 정보 없음
            aiExplanation = buildAiExplanation(aiResponse)
        )
    }

    /**
     * AI 응답에서 설명 텍스트 생성
     */
    private fun buildAiExplanation(aiResponse: GetAIAnalysisResponse?): String? {
        if (aiResponse == null) return null
        
        val parts = mutableListOf<String>()
        
        // 요약
        aiResponse.riskSummary?.let { parts.add(it) }
        
        // 위험 요소
        if (!aiResponse.riskFactors.isNullOrEmpty()) {
            parts.add("위험 요소: ${aiResponse.riskFactors.joinToString(", ")}")
        }
        
        return if (parts.isNotEmpty()) parts.joinToString("\n\n") else aiResponse.aiInsight
    }

    /**
     * 분석 소요 시간 포맷팅
     */
    fun formatProcessingTime(millis: Long?): String {
        if (millis == null) return ""
        return when {
            millis < 1000 -> "${millis}ms"
            else -> String.format("%.1f초", millis / 1000.0)
        }
    }
}
