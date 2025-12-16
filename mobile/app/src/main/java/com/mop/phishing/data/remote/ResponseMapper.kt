package com.mop.phishing.data.remote

<<<<<<< HEAD
import android.util.Log
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
import com.mop.phishing.ui.components.RiskLevel
import com.mop.phishing.ui.screens.detail.AnalysisDetails
import com.mop.phishing.ui.screens.detail.AnalysisResult

/**
 * API 응답 → UI 모델 변환기
 * 백엔드 응답을 UI에서 사용하는 형태로 변환
 */
object ResponseMapper {

<<<<<<< HEAD
    private const val TAG = "ResponseMapper"

=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD
     * ML 0.4 + VT 0.6 가중치로 실제 점수 계산 (v2 수정)
     *
     * 공식: finalScore = (ML점수 * 0.4) + (VT점수 * 0.6)
     * - ML점수: 0~100% (로컬 온디바이스 ML)
     * - VT점수: (악성*10 + 의심*5) / 총엔진수 * 100, 최대 100%
     * 
     * @param actualLocalMlScore 로컬 ML 점수 (0.0~1.0), null이면 백엔드 응답 사용
     */
    private fun calculateAdjustedScore(
        response: AnalyzeSuspiciousUrlResponse,
        actualLocalMlScore: Float? = null
    ): Pair<Int, RiskLevel> {
        // 🔧 핵심 수정: 실제 로컬 ML 점수 우선 사용
        val mlScore = actualLocalMlScore?.toDouble() 
            ?: response.breakdown?.localMlScore 
            ?: response.finalRiskScore 
            ?: 0.0
        
        Log.d(TAG, "🔍 ML 점수 소스: actualLocal=${actualLocalMlScore}, backend=${response.breakdown?.localMlScore}, using=$mlScore")
        val vtMalicious = response.virusTotal?.malicious ?: 0
        val vtSuspicious = response.virusTotal?.suspicious ?: 0
        val vtTotal = response.breakdown?.virusTotalStats?.total
            ?: ((response.virusTotal?.malicious ?: 0) +
                (response.virusTotal?.harmless ?: 0) +
                (response.virusTotal?.undetected ?: 0) +
                (response.virusTotal?.suspicious ?: 0))

        val vtStatus = response.virusTotal?.status

        // ML 점수 (0~100)
        val mlScorePercent = (mlScore * 100).coerceIn(0.0, 100.0)

        // VT 점수 계산 (0~100)
        // 악성 1개 = 10점, 의심 1개 = 5점, 전체 엔진 대비 비율
        val vtScorePercent = if (vtTotal > 0) {
            val rawVtScore = (vtMalicious * 10.0 + vtSuspicious * 5.0) / vtTotal * 100.0
            rawVtScore.coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        // 🔧 가중치 조정: ML 0.4 + VT 0.6 (로컬 ML 비중 증가)
        val ML_WEIGHT = 0.4
        val VT_WEIGHT = 0.6

        // VT 결과 유무에 따른 점수 계산
        val finalScore = if (vtTotal > 0) {
            // VT 결과가 있으면 ML 0.3 + VT 0.7 가중치 적용
            (mlScorePercent * ML_WEIGHT + vtScorePercent * VT_WEIGHT).toInt()
        } else {
            // VT 결과가 없으면 ML 점수 100% 사용
            Log.d(TAG, "  ⚠️ VT 결과 없음 - ML 점수만 사용")
            mlScorePercent.toInt()
        }

        val adjustedScore = finalScore.coerceIn(0, 100)

        Log.d(TAG, "점수 계산 (ML $ML_WEIGHT + VT $VT_WEIGHT):")
        Log.d(TAG, "  ML 점수: ${mlScorePercent.toInt()}%")
        Log.d(TAG, "  VT: 악성=$vtMalicious, 의심=$vtSuspicious, 총=$vtTotal, 상태=$vtStatus")
        Log.d(TAG, "  VT 점수: ${vtScorePercent.toInt()}%")
        Log.d(TAG, "  계산: ${mlScorePercent.toInt()}*$ML_WEIGHT + ${vtScorePercent.toInt()}*$VT_WEIGHT = $adjustedScore")

        // 점수로 RiskLevel 결정 (30점 이하 = 안전, 31~59 = 주의, 60 이상 = 위험)
        val adjustedLevel = when {
            adjustedScore <= 30 -> RiskLevel.SAFE
            adjustedScore < 60 -> RiskLevel.CAUTION
            else -> RiskLevel.DANGER
        }

        Log.d(TAG, "  최종: 점수=$adjustedScore, 레벨=$adjustedLevel")

        return Pair(adjustedScore, adjustedLevel)
    }

    /**
     * API 응답 → UI AnalysisResult 변환 (보정 적용)
     * @param actualLocalMlScore 실제 로컬 ML 점수 (null이면 백엔드 응답값 사용)
     */
    fun mapToAnalysisResult(
        response: AnalyzeSuspiciousUrlResponse,
        aiResponse: GetAIAnalysisResponse? = null,
        actualLocalMlScore: Float? = null
    ): AnalysisResult {
        Log.d(TAG, "━━━━ mapToAnalysisResult 시작 ━━━━")
        Log.d(TAG, "  actualLocalMlScore 전달값: $actualLocalMlScore")
        
        // 🔧 핵심 수정: actualLocalMlScore를 calculateAdjustedScore에 전달
        val (adjustedScore, adjustedLevel) = calculateAdjustedScore(response, actualLocalMlScore)
        
        Log.d(TAG, "  최종 결과: score=$adjustedScore, level=$adjustedLevel")
        Log.d(TAG, "━━━━ mapToAnalysisResult 완료 ━━━━")

        return AnalysisResult(
            riskLevel = adjustedLevel,
            riskScore = adjustedScore,
            recommendation = getRecommendation(adjustedLevel, aiResponse?.recommendations),
            canAccess = canAccess(adjustedLevel),
            details = mapToAnalysisDetails(response, aiResponse, adjustedScore, actualLocalMlScore)
=======
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
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        )
    }

    /**
     * API 응답 → UI AnalysisDetails 변환
<<<<<<< HEAD
     * @param actualLocalMlScore 실제 로컬 ML 점수 (null이면 백엔드 응답값 사용)
     */
    fun mapToAnalysisDetails(
        response: AnalyzeSuspiciousUrlResponse,
        aiResponse: GetAIAnalysisResponse? = null,
        adjustedFinalScore: Int? = null,
        actualLocalMlScore: Float? = null
    ): AnalysisDetails {
        // VirusTotal 전체 엔진 수 계산
        val vtTotal = response.breakdown?.virusTotalStats?.total
            ?: ((response.virusTotal?.malicious ?: 0) +
                (response.virusTotal?.harmless ?: 0) +
                (response.virusTotal?.undetected ?: 0) +
                (response.virusTotal?.suspicious ?: 0))

        // 🔧 실제 로컬 ML 점수 우선 사용 (null이면 백엔드 응답값 사용)
        val localMlScoreToUse = actualLocalMlScore ?: response.breakdown?.localMlScore?.toFloat() ?: 0f

        Log.d(TAG, "mapToAnalysisDetails: actualLocalMlScore=$actualLocalMlScore, backendScore=${response.breakdown?.localMlScore}, using=$localMlScoreToUse")

        return AnalysisDetails(
            localMlScore = localMlScoreToUse,
            virusTotalMalicious = response.virusTotal?.malicious ?: 0,
            virusTotalSuspicious = response.virusTotal?.suspicious ?: 0,
            virusTotalHarmless = response.virusTotal?.harmless ?: 0,  // 🆕 정상 탐지 개수
            virusTotalTotal = vtTotal,
            redirectCount = 0,
            finalScore = adjustedFinalScore ?: mapScoreToPercent(response.finalRiskScore),
            aiExplanation = buildAiExplanation(aiResponse),
            expandedUrl = response.expandedUrl  // 🆕 단축 URL 해제 결과
=======
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
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
