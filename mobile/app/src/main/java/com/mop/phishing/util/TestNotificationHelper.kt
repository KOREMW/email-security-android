package com.mop.phishing.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 테스트 알림 생성 헬퍼
 * 앱의 알림 분석 기능을 테스트하기 위한 샘플 알림 생성
 */
object TestNotificationHelper {

    private const val TAG = "TestNotificationHelper"
    private const val TEST_CHANNEL_ID = "test_notification_channel"
    private const val TEST_CHANNEL_NAME = "테스트 알림"

    // 테스트용 URL - 고정 URL 사용 (매번 같은 URL로 캐싱 테스트 가능)
    private val SAFE_URLS = listOf(
        "https://www.google.com",
        "https://www.naver.com",
        "https://www.github.com"
    )

    private val SUSPICIOUS_URLS = listOf(
        "http://bit.ly/suspicious-test",
        "https://login-verify.example.com/auth",
        "http://192.168.1.1/admin"
    )

    private val DANGEROUS_URLS = listOf(
        "http://malware.testing.google.test/testing/malware/",
        "http://testsafebrowsing.appspot.com/s/phishing.html",
        "https://www.wicar.org/test-malware.html"
    )

    enum class TestScenario(val title: String, val description: String) {
        SAFE_EMAIL("안전한 이메일", "정상적인 링크가 포함된 이메일"),
        SUSPICIOUS_SMS("의심스러운 문자", "피싱 의심 링크가 포함된 문자"),
        DANGEROUS_KAKAO("위험한 카카오톡", "악성 링크가 포함된 카카오톡 메시지")
    }

    fun createTestChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TEST_CHANNEL_ID,
                TEST_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "알림 분석 기능 테스트용 채널"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTestNotification(context: Context, scenario: TestScenario): Int {
        createTestChannel(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random.nextInt(10000, 99999)

        val (title, content) = when (scenario) {
            TestScenario.SAFE_EMAIL -> Pair(
                "[Gmail] 회의 일정 안내",
                "내일 오후 2시 회의가 있습니다. 참석 링크: ${SAFE_URLS.random()}"
            )
            TestScenario.SUSPICIOUS_SMS -> Pair(
                "[Web발신] 택배 배송 알림",
                "고객님의 택배가 도착했습니다. 배송 확인: ${SUSPICIOUS_URLS.random()}"
            )
            TestScenario.DANGEROUS_KAKAO -> Pair(
                "카카오톡",
                "[긴급] 계정이 해킹되었습니다. 지금 바로 확인하세요: ${DANGEROUS_URLS.random()}"
            )
        }

        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)

        return notificationId
    }

    /**
     * URL 분석 테스트 수행
     * NonCancellable 컨텍스트로 실행하여 화면 전환 시에도 취소되지 않음
     */
    suspend fun testUrlAnalysis(
        context: Context,
        url: String,
        scenario: TestScenario
    ) = withContext(NonCancellable) {
        val repository = com.mop.phishing.data.local.NotificationRepository.getInstance(context)

        val (title, content) = when (scenario) {
            TestScenario.SAFE_EMAIL -> Pair(
                "[Gmail] 안전한 이메일 테스트",
                "이것은 안전한 URL 테스트입니다: $url"
            )
            TestScenario.SUSPICIOUS_SMS -> Pair(
                "[SMS] 의심스러운 문자 테스트",
                "이것은 의심스러운 URL 테스트입니다: $url"
            )
            TestScenario.DANGEROUS_KAKAO -> Pair(
                "[카카오톡] 위험한 메시지 테스트",
                "이것은 위험한 URL 테스트입니다: $url"
            )
        }

        val source = when (scenario) {
            TestScenario.SAFE_EMAIL -> com.mop.phishing.ui.components.NotificationSource.GMAIL
            TestScenario.SUSPICIOUS_SMS -> com.mop.phishing.ui.components.NotificationSource.SMS
            TestScenario.DANGEROUS_KAKAO -> com.mop.phishing.ui.components.NotificationSource.KAKAOTALK
        }

        val notificationId = repository.saveNotification(
            source = source,
            title = title,
            content = content,
            url = url,
            sourcePackage = "com.mop.phishing.test"
        )

        // ML 분석 수행 - 안전하게 처리
        try {
            // UrlClassifier 로드 시도 (실패해도 계속 진행)
            val classifier = try {
                com.mop.phishing.ml.UrlClassifier.getInstance(context)
            } catch (e: Exception) {
                Log.e(TAG, "UrlClassifier 로드 실패 (무시하고 계속): ${e.message}")
                null
            }
            
            val localScore = classifier?.predict(url) ?: 0.5f

            var riskLevel: com.mop.phishing.ui.components.RiskLevel = com.mop.phishing.ui.components.RiskLevel.UNKNOWN
            var riskScore = (localScore * 100).toInt()
            var vtMalicious = 0
            var vtSuspicious = 0
            var vtTotal = 0
            var finalScore = riskScore
            var aiExplanation: String? = null
            var recommendationText: String? = null
            var canAccessUrl = true

            try {
                val response = com.mop.phishing.data.remote.CloudFunctionRepository.analyzeSuspiciousUrl(
                    url = url,
                    source = "test",
                    localRiskScore = localScore.toDouble()
                )

                riskLevel = com.mop.phishing.data.remote.ResponseMapper.mapSuspiciousLevelToRiskLevel(response.suspiciousLevel)
                riskScore = com.mop.phishing.data.remote.ResponseMapper.mapScoreToPercent(response.finalRiskScore)
                vtMalicious = response.virusTotal?.malicious ?: 0
                vtSuspicious = response.virusTotal?.suspicious ?: 0
                vtTotal = response.breakdown?.virusTotalStats?.total 
                    ?: ((response.virusTotal?.malicious ?: 0) + 
                        (response.virusTotal?.harmless ?: 0) + 
                        (response.virusTotal?.undetected ?: 0) +
                        (response.virusTotal?.suspicious ?: 0))
                finalScore = riskScore

                Log.d(TAG, "서버 분석 성공: url=$url, score=$riskScore, level=$riskLevel")

                // AI 분석 요청 추가
                try {
                    Log.d(TAG, "AI 분석 요청 시작: url=$url")
                    val aiResponse = com.mop.phishing.data.remote.CloudFunctionRepository.getAIAnalysis(url, response)
                    aiExplanation = aiResponse.riskSummary
                    // recommendations는 List<String>이므로 첫 번째 항목 또는 조합
                    recommendationText = aiResponse.recommendations?.joinToString("\n") 
                        ?: getDefaultRecommendation(riskLevel)
                    // canAccess는 suspiciousLevel로 판단
                    canAccessUrl = aiResponse.suspiciousLevel?.uppercase() == "SAFE"
                    Log.d(TAG, "AI 분석 완료: summary=${aiExplanation?.take(50)}...")
                } catch (aiError: Exception) {
                    Log.w(TAG, "AI 분석 실패 (무시): ${aiError.message}")
                    // AI 분석 실패해도 계속 진행 - 기본 권장사항 설정
                    recommendationText = getDefaultRecommendation(riskLevel)
                    canAccessUrl = riskLevel == com.mop.phishing.ui.components.RiskLevel.SAFE
                }

            } catch (e: Exception) {
                Log.w(TAG, "서버 분석 실패, 로컬 점수 사용: ${e.message}")
                riskLevel = when {
                    localScore >= 0.7f -> com.mop.phishing.ui.components.RiskLevel.DANGER
                    localScore >= 0.4f -> com.mop.phishing.ui.components.RiskLevel.CAUTION
                    else -> com.mop.phishing.ui.components.RiskLevel.SAFE
                }
                recommendationText = getDefaultRecommendation(riskLevel)
                canAccessUrl = riskLevel == com.mop.phishing.ui.components.RiskLevel.SAFE
            }

            repository.updateAnalysisResult(
                id = notificationId,
                riskLevel = riskLevel,
                riskScore = riskScore,
                localMlScore = localScore,
                vtMalicious = vtMalicious,
                vtSuspicious = vtSuspicious,
                vtTotal = vtTotal,
                finalScore = finalScore,
                aiExplanation = aiExplanation,
                recommendation = recommendationText,
                canAccess = canAccessUrl
            )

            Log.d(TAG, "테스트 분석 완료: id=$notificationId, score=$riskScore")

        } catch (e: Exception) {
            Log.e(TAG, "테스트 분석 실패: ${e.message}", e)
        }
    }

    /**
     * 기본 권장사항 반환
     */
    private fun getDefaultRecommendation(riskLevel: com.mop.phishing.ui.components.RiskLevel): String {
        return when (riskLevel) {
            com.mop.phishing.ui.components.RiskLevel.SAFE -> "안전한 링크입니다. 정상적으로 접속하셔도 됩니다."
            com.mop.phishing.ui.components.RiskLevel.CAUTION -> "주의가 필요한 링크입니다. 개인정보 입력에 주의하세요."
            com.mop.phishing.ui.components.RiskLevel.DANGER -> "위험한 링크입니다. 접속을 권장하지 않습니다."
            com.mop.phishing.ui.components.RiskLevel.UNKNOWN -> "분석 결과를 확인해주세요."
        }
    }

    /**
     * 빠른 테스트 - 캐시 확인 후 분석 수행
     * NonCancellable 컨텍스트로 실행하여 화면 전환 시에도 취소되지 않음
     */
    suspend fun quickTest(context: Context, scenario: TestScenario): TestResult = withContext(NonCancellable) {
        val url = when (scenario) {
            TestScenario.SAFE_EMAIL -> SAFE_URLS.first()
            TestScenario.SUSPICIOUS_SMS -> SUSPICIOUS_URLS.first()
            TestScenario.DANGEROUS_KAKAO -> DANGEROUS_URLS.first()
        }

        val repository = com.mop.phishing.data.local.NotificationRepository.getInstance(context)

        try {
            // 캐시 확인
            val cachedResult = repository.getAnalyzedResultByUrl(url)
            if (cachedResult != null) {
                Log.d(TAG, "캐시된 결과 사용: url=$url, score=${cachedResult.riskScore}")
                return@withContext TestResult(
                    url = url,
                    isCached = true,
                    riskScore = cachedResult.riskScore,
                    riskLevel = cachedResult.riskLevel,
                    vtMalicious = cachedResult.virusTotalMalicious,
                    vtSuspicious = cachedResult.virusTotalSuspicious,
                    error = null
                )
            }

            // 새 분석 수행
            Log.d(TAG, "새 분석 수행: url=$url")
            testUrlAnalysis(context, url, scenario)

            val newResult = repository.getAnalyzedResultByUrl(url)
            TestResult(
                url = url,
                isCached = false,
                riskScore = newResult?.riskScore ?: 0,
                riskLevel = newResult?.riskLevel ?: "UNKNOWN",
                vtMalicious = newResult?.virusTotalMalicious ?: 0,
                vtSuspicious = newResult?.virusTotalSuspicious ?: 0,
                error = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "quickTest 실패: ${e.message}", e)
            TestResult(
                url = url,
                isCached = false,
                riskScore = 0,
                riskLevel = "ERROR",
                vtMalicious = 0,
                vtSuspicious = 0,
                error = e.message
            )
        }
    }

    data class TestResult(
        val url: String,
        val isCached: Boolean,
        val riskScore: Int,
        val riskLevel: String,
        val vtMalicious: Int,
        val vtSuspicious: Int,
        val error: String? = null
    )
}
