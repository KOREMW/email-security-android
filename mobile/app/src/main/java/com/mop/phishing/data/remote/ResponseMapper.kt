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
        // VirusTotal 전체 엔진 수 계산
        val vtTotal = response.breakdown?.virusTotalStats?.total
            ?: ((response.virusTotal?.malicious ?: 0) + 
                (response.virusTotal?.harmless ?: 0) + 
                (response.virusTotal?.undetected ?: 0) +
                (response.virusTotal?.suspicious ?: 0))
        
        return AnalysisDetails(
            localMlScore = response.breakdown?.localMlScore?.toFloat() ?: 0f,
            virusTotalMalicious = response.virusTotal?.malicious ?: 0,
            virusTotalSuspicious = response.virusTotal?.suspicious ?: 0,
            virusTotalTotal = vtTotal,
            redirectCount = 0,  // 백엔드에서 redirect 정보 없음
            finalScore = mapScoreToPercent(response.finalRiskScore),  // 최종 점수 추가
            aiExplanation = buildAiExplanation(aiResponse)
        )
    }

    /**
     * AI 응답에서 상세 설명 텍스트 생성 (개선 버전)
     */
    private fun buildAiExplanation(aiResponse: GetAIAnalysisResponse?): String? {
        if (aiResponse == null) return null

        val parts = mutableListOf<String>()

        // 1. 핵심 요약
        aiResponse.riskSummary?.let {
            parts.add("📌 $it")
        }

        // 2. 피싱 유형 (새로 추가!)
        aiResponse.phishingType?.let { type ->
            if (type != "정상 사이트") {
                parts.add("🎯 피싱 유형: $type")
            }
        }

        // 3. 사칭 대상 브랜드 (새로 추가!)
        aiResponse.targetedBrand?.let { brand ->
            parts.add("⚠️ 사칭 대상: $brand")
        }

        // 4. 사용된 피싱 기법 (새로 추가!)
        if (!aiResponse.phishingTechniques.isNullOrEmpty()) {
            parts.add("🔧 피싱 기법: ${aiResponse.phishingTechniques.joinToString(", ")}")
        }

        // 5. 기술적 분석 (새로 추가!)
        aiResponse.technicalAnalysis?.let {
            parts.add("🔍 기술 분석: $it")
        }

        // 6. 예상 공격 시나리오 (새로 추가!)
        aiResponse.attackScenario?.let {
            parts.add("⚡ 공격 시나리오: $it")
        }

        // 7. 위험 요소
        if (!aiResponse.riskFactors.isNullOrEmpty()) {
            parts.add("🚨 위험 요소:\n• ${aiResponse.riskFactors.joinToString("\n• ")}")
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString("\n\n")
        } else {
            aiResponse.aiInsight
        }
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
