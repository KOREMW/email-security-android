package com.mop.phishing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
<<<<<<< HEAD
=======
import android.media.AudioAttributes
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mop.phishing.data.local.NotificationRepository
import com.mop.phishing.data.local.SettingsRepository
import com.mop.phishing.data.remote.CloudFunctionRepository
import com.mop.phishing.data.remote.ResponseMapper
import com.mop.phishing.ml.UrlClassifier
import com.mop.phishing.ui.components.NotificationSource
import com.mop.phishing.ui.components.RiskLevel
import com.mop.phishing.util.UrlBlockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 알림 리스너 서비스
 * 메일/메신저 알림에서 URL을 추출하고 분석
<<<<<<< HEAD
 * 
 * 🆕 전체 분석 파이프라인:
 * 1. 알림에서 URL 추출
 * 2. 온디바이스 ML 분석 (TensorFlow Lite)
 * 3. 서버 API 호출 (VirusTotal 포함)
 * 4. AI 분석 보고서 생성 (OpenAI)
 * 5. 결과 저장 및 푸시 알림
=======
 * 분석 결과를 Repository에 저장하여 UI에 표시
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
 */
class MailNotificationListener : NotificationListenerService() {

    companion object {
<<<<<<< HEAD
        private var isServiceConnected = false
        
        fun isConnected(): Boolean = isServiceConnected
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        private const val TAG = "NotiListener"
        private const val CHANNEL_ID = "security_alert"
        private const val CHANNEL_NAME = "보안 알림"
        
<<<<<<< HEAD
        // 🚨 빌드 확인용 - 이 값이 Logcat에 보이면 새 APK가 설치된 것
        private const val BUILD_ID = "2025-12-16-FIX-V3"
        
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        // 감시할 앱 패키지 목록
        private val MONITORED_PACKAGES = mapOf(
            "com.google.android.gm" to NotificationSource.GMAIL,
            "com.samsung.android.email.provider" to NotificationSource.GMAIL,
<<<<<<< HEAD
            "com.nhn.android.mail" to NotificationSource.NAVER,
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
            "com.kakao.talk" to NotificationSource.KAKAOTALK,
            "com.android.mms" to NotificationSource.SMS,
            "com.samsung.android.messaging" to NotificationSource.SMS
        )

        // 위험 임계값
        private const val DANGER_THRESHOLD = 0.7f
        private const val CAUTION_THRESHOLD = 0.4f
<<<<<<< HEAD

        // 중복 방지: 최근 처리한 URL 캐시 (URL -> 처리 시간)
        private val recentlyProcessedUrls = mutableMapOf<String, Long>()
        private const val DUPLICATE_CHECK_INTERVAL_MS = 30_000L // 30초 내 동일 URL 무시
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    }

    private var classifier: UrlClassifier? = null
    private var repository: NotificationRepository? = null
    private var settingsRepository: SettingsRepository? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
<<<<<<< HEAD
=======

        // Repository 초기화
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        repository = NotificationRepository.getInstance(this)
        settingsRepository = SettingsRepository.getInstance(this)
        Log.d(TAG, "Repository 초기화 완료")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
<<<<<<< HEAD
        isServiceConnected = true
        
        Log.w(TAG, "████████████████████████████████████████")
        Log.w(TAG, "██ NotiListener BUILD_ID: $BUILD_ID ██")
        Log.w(TAG, "████████████████████████████████████████")
        Log.d(TAG, "알림 리스너 연결됨")

        try {
            Log.d(TAG, "UrlClassifier 초기화 시작...")
            classifier = UrlClassifier.getInstance(this)
            Log.d(TAG, "✅ UrlClassifier 싱글톤 연결 완료 (VERSION=${UrlClassifier.VERSION})")
        } catch (e: Exception) {
            Log.e(TAG, "❌ UrlClassifier 초기화 실패: ${e.message}", e)
=======
        Log.d(TAG, "알림 리스너 연결됨")

        try {
            // 싱글톤 사용으로 메모리 효율 개선
            classifier = UrlClassifier.getInstance(this)
            Log.d(TAG, "UrlClassifier 싱글톤 연결 완료")
        } catch (e: Exception) {
            Log.e(TAG, "UrlClassifier 초기화 실패: ${e.message}")
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        }
    }

    override fun onListenerDisconnected() {
<<<<<<< HEAD
        isServiceConnected = false
        Log.d(TAG, "알림 리스너 연결 해제됨")
        classifier = null
        
        try {
            super.onListenerDisconnected()
        } catch (e: Exception) {
            Log.w(TAG, "onListenerDisconnected 예외 (무시): ${e.message}")
        }
    }
    
    override fun onDestroy() {
        isServiceConnected = false
        classifier = null
        Log.d(TAG, "서비스 종료됨")
        
        try {
            super.onDestroy()
        } catch (e: Exception) {
            Log.w(TAG, "onDestroy 예외 (무시): ${e.message}")
        }
=======
        super.onListenerDisconnected()
        Log.d(TAG, "알림 리스너 연결 해제됨")
        // 싱글톤이므로 close하지 않음 (다른 곳에서 사용 중일 수 있음)
        classifier = null
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        
<<<<<<< HEAD
=======
        // 모니터링 대상 앱인지 확인 및 소스 타입 가져오기
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        val notificationSource = getNotificationSource(pkg)
        if (notificationSource == null) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullContent = "$title $text $bigText"
        
        Log.d(TAG, "알림 수신: pkg=$pkg, title=$title")

<<<<<<< HEAD
=======
        // URL 추출
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        val urls = extractUrls(fullContent)
        if (urls.isEmpty()) {
            Log.d(TAG, "URL 없음, 건너뜀")
            return
        }

        Log.d(TAG, "발견된 URL: ${urls.size}개")

<<<<<<< HEAD
        urls.forEach { url ->
            // 중복 체크: 최근 30초 내 동일 URL 처리했으면 건너뜀
            if (isRecentlyProcessed(url)) {
                Log.d(TAG, "중복 URL 건너뜀: $url")
                return@forEach
            }

            // 처리 시간 기록
            markAsProcessed(url)

=======
        // 각 URL 분석
        urls.forEach { url ->
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
            analyzeAndSaveUrl(
                url = url,
                source = notificationSource,
                sourcePackage = pkg,
                title = title,
                content = fullContent
            )
        }
    }

<<<<<<< HEAD
    /**
     * URL이 최근에 처리되었는지 확인
     */
    private fun isRecentlyProcessed(url: String): Boolean {
        val lastProcessedTime = recentlyProcessedUrls[url] ?: return false
        val now = System.currentTimeMillis()
        return (now - lastProcessedTime) < DUPLICATE_CHECK_INTERVAL_MS
    }

    /**
     * URL을 처리됨으로 표시
     */
    private fun markAsProcessed(url: String) {
        val now = System.currentTimeMillis()
        recentlyProcessedUrls[url] = now

        // 오래된 항목 정리 (메모리 관리)
        val expiredTime = now - DUPLICATE_CHECK_INTERVAL_MS
        recentlyProcessedUrls.entries.removeIf { it.value < expiredTime }
    }

=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

<<<<<<< HEAD
=======
    /**
     * 패키지명으로 NotificationSource 반환
     */
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    private fun getNotificationSource(pkg: String): NotificationSource? {
        for ((key, source) in MONITORED_PACKAGES) {
            if (pkg.contains(key) || key.contains(pkg)) {
                return source
            }
        }
        return null
    }

<<<<<<< HEAD
    private fun extractUrls(text: String): List<String> {
=======
    /**
     * URL 추출 (정규식)
     * 개선된 정규식: 한글, 틸드, 플러스 등 더 많은 문자 지원
     */
    private fun extractUrls(text: String): List<String> {
        // 기존: (https?://[\w./?=&%\-#@:]+)
        // 개선: 틸드(~), 플러스(+), 세미콜론(;), 콤마(,), 느낌표(!), 별표(*),
        //       작은따옴표('), 괄호(()), 대괄호([]), 유니코드 문자 지원
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        val regex = Regex(
            "(https?://[\\w./?=&%\\-#@:~+;,!*'()\\[\\]\\u0080-\\uFFFF]+)",
            RegexOption.IGNORE_CASE
        )
        return regex.findAll(text)
<<<<<<< HEAD
            .map { it.value.trimEnd('.', ',', ')', ']', '\'', '!') }
=======
            .map { it.value.trimEnd('.', ',', ')', ']', '\'', '!') } // 문장 끝 부호 제거
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
            .filter { isValidUrl(it) }
            .distinct()
            .toList()
    }

<<<<<<< HEAD
=======
    /**
     * URL 유효성 검사
     */
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.host != null && uri.host.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
<<<<<<< HEAD
     * 🆕 URL 전체 분석 파이프라인
     * 1. DB 저장
     * 2. ML 분석
     * 3. 서버 API (VirusTotal)
     * 4. AI 분석 (OpenAI)
     * 5. 결과 업데이트
=======
     * URL 분석 및 Repository 저장
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
     */
    private fun analyzeAndSaveUrl(
        url: String,
        source: NotificationSource,
        sourcePackage: String,
        title: String,
        content: String
    ) {
        serviceScope.launch {
            try {
                val repo = repository ?: return@launch
                val settings = settingsRepository

                // 자동 분석 설정 확인
                if (settings != null && !settings.isAutoAnalysisEnabled()) {
                    Log.d(TAG, "자동 분석이 비활성화되어 있습니다. 건너뜀: $url")
                    return@launch
                }

<<<<<<< HEAD
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🔍 URL 분석 시작: $url")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // Step 1: Repository에 알림 저장 (미분석 상태)
=======
                // 1. Repository에 알림 저장 (미분석 상태)
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                val notificationId = repo.saveNotification(
                    source = source,
                    title = title,
                    content = content,
                    url = url,
                    sourcePackage = sourcePackage
                )
<<<<<<< HEAD
                Log.d(TAG, "📝 Step 1: 알림 저장 완료 (id=$notificationId)")

                // Step 2: 로컬 ML 분석
                Log.d(TAG, "🧠 Step 2: 온디바이스 ML 분석 시작...")
                Log.d(TAG, "  classifier 상태: ${if (classifier == null) "❌ NULL" else "✅ OK"}")

                val localScore = classifier?.let { cls ->
                    try {
                        val score = cls.predict(url)
                        Log.d(TAG, "  ✅ predict() 성공: score=$score")
                        score
                    } catch (e: Exception) {
                        Log.e(TAG, "  ❌ predict() 실패: ${e.message}", e)
                        0.5f
                    }
                } ?: run {
                    Log.e(TAG, "  ❌ classifier가 null! 기본값 0.5 사용")
                    0.5f
                }

                Log.d(TAG, "🧠 Step 2: ML 분석 완료 - 점수=${(localScore * 100).toInt()}%")

                // 결과 변수 초기화
=======
                Log.d(TAG, "알림 저장 완료: id=$notificationId")

                // 2. 로컬 ML 분석
                val localScore = classifier?.predict(url) ?: 0.5f
                Log.d(TAG, "URL=$url, 로컬 점수=$localScore")

                // 3. 서버 분석 요청
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                var riskLevel = RiskLevel.UNKNOWN
                var riskScore = (localScore * 100).toInt()
                var vtMalicious = 0
                var vtSuspicious = 0
<<<<<<< HEAD
                var vtTotal = 0
                var finalScore = riskScore
                var aiExplanation: String? = null
                var recommendation: String? = null
                var canAccess = true
                
                // Step 3: 서버 API 호출 (VirusTotal 포함)
                try {
                    Log.d(TAG, "🌐 Step 3: 서버 API 호출 시작 (VirusTotal 포함)...")
=======
                var aiExplanation: String? = null
                
                try {
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    val response = CloudFunctionRepository.analyzeSuspiciousUrl(
                        url = url,
                        source = "notification",
                        localRiskScore = localScore.toDouble()
                    )

<<<<<<< HEAD
                    Log.d(TAG, "🌐 Step 3: 서버 응답 수신")
                    Log.d(TAG, "   - suspicious: ${response.suspicious}")
                    Log.d(TAG, "   - finalRiskScore: ${response.finalRiskScore}")
                    Log.d(TAG, "   - suspiciousLevel: ${response.suspiciousLevel}")
                    Log.d(TAG, "   - VT malicious: ${response.virusTotal?.malicious}")
                    Log.d(TAG, "   - VT suspicious: ${response.virusTotal?.suspicious}")

                    // 서버 응답으로 점수 계산 (보정 적용) - 🔧 실제 로컬 ML 점수 전달
                    val analysisResult = ResponseMapper.mapToAnalysisResult(response, null, localScore)
                    riskLevel = analysisResult.riskLevel
                    riskScore = analysisResult.riskScore
                    vtMalicious = response.virusTotal?.malicious ?: 0
                    vtSuspicious = response.virusTotal?.suspicious ?: 0
                    vtTotal = response.breakdown?.virusTotalStats?.total
                        ?: ((response.virusTotal?.malicious ?: 0) +
                            (response.virusTotal?.harmless ?: 0) +
                            (response.virusTotal?.undetected ?: 0) +
                            (response.virusTotal?.suspicious ?: 0))
                    finalScore = analysisResult.riskScore

                    // Step 4: AI 분석 요청 (OpenAI)
                    try {
                        Log.d(TAG, "🤖 Step 4: AI 분석 보고서 생성 시작...")
                        val aiResponse = CloudFunctionRepository.getAIAnalysis(url, response)
                        
                        // AI 응답에서 상세 정보 추출
                        aiExplanation = buildAiExplanation(aiResponse)
                        recommendation = aiResponse.recommendations?.joinToString("\n") 
                            ?: getDefaultRecommendation(riskLevel)
                        canAccess = aiResponse.suspiciousLevel?.uppercase() == "SAFE"
                        
                        Log.d(TAG, "🤖 Step 4: AI 분석 완료")
                        Log.d(TAG, "   - riskSummary: ${aiResponse.riskSummary?.take(50)}...")
                        Log.d(TAG, "   - phishingType: ${aiResponse.phishingType}")
                        Log.d(TAG, "   - targetedBrand: ${aiResponse.targetedBrand}")
                        
                    } catch (aiError: Exception) {
                        Log.w(TAG, "🤖 Step 4: AI 분석 실패 (무시하고 계속): ${aiError.message}")
                        recommendation = getDefaultRecommendation(riskLevel)
                        canAccess = riskLevel == RiskLevel.SAFE
                    }
                    
                } catch (serverError: Exception) {
                    Log.w(TAG, "🌐 Step 3: 서버 분석 실패, 로컬 점수로 판단: ${serverError.message}")
                    
=======
                    Log.d(TAG, "서버 응답: suspicious=${response.suspicious}, score=${response.finalRiskScore}")

                    // 서버 응답으로 업데이트
                    riskLevel = ResponseMapper.mapSuspiciousLevelToRiskLevel(response.suspiciousLevel)
                    riskScore = ResponseMapper.mapScoreToPercent(response.finalRiskScore)
                    vtMalicious = response.virusTotal?.malicious ?: 0
                    vtSuspicious = response.virusTotal?.suspicious ?: 0
                    
                } catch (serverError: Exception) {
                    Log.w(TAG, "서버 분석 실패, 로컬 점수로 판단: ${serverError.message}")
                    
                    // 서버 실패 시 로컬 점수로 위험도 결정
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    riskLevel = when {
                        localScore >= DANGER_THRESHOLD -> RiskLevel.DANGER
                        localScore >= CAUTION_THRESHOLD -> RiskLevel.CAUTION
                        else -> RiskLevel.SAFE
                    }
                    riskScore = (localScore * 100).toInt()
<<<<<<< HEAD
                    finalScore = riskScore
                    recommendation = getDefaultRecommendation(riskLevel)
                    canAccess = riskLevel == RiskLevel.SAFE
                }

                // Step 5: 분석 결과 DB 업데이트
                Log.d(TAG, "💾 Step 5: 분석 결과 저장...")
=======
                }

                // 4. 분석 결과 업데이트
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                repo.updateAnalysisResult(
                    id = notificationId,
                    riskLevel = riskLevel,
                    riskScore = riskScore,
                    localMlScore = localScore,
                    vtMalicious = vtMalicious,
                    vtSuspicious = vtSuspicious,
<<<<<<< HEAD
                    vtTotal = vtTotal,
                    finalScore = finalScore,
                    aiExplanation = aiExplanation,
                    recommendation = recommendation,
                    canAccess = canAccess
                )
                
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "✅ 분석 완료: $riskLevel ($riskScore%)")
                Log.d(TAG, "   - ML: ${(localScore * 100).toInt()}%")
                Log.d(TAG, "   - VT: 악성=$vtMalicious, 의심=$vtSuspicious")
                Log.d(TAG, "   - AI 보고서: ${if (aiExplanation != null) "생성됨" else "없음"}")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // Step 6: 고위험 자동 차단
                if (riskLevel == RiskLevel.DANGER && settings?.isAutoBlockHighRiskEnabled() == true) {
                    UrlBlockManager.blockUrl(this@MailNotificationListener, url)
                    Log.d(TAG, "🚫 고위험 URL 자동 차단됨: $url")
                }

                // Step 7: 푸시 알림 표시
=======
                    aiExplanation = aiExplanation
                )
                Log.d(TAG, "분석 결과 저장 완료: $riskLevel ($riskScore%)")

                // 5. 고위험 자동 차단 설정 확인
                if (riskLevel == RiskLevel.DANGER && settings?.isAutoBlockHighRiskEnabled() == true) {
                    UrlBlockManager.blockUrl(this@MailNotificationListener, url)
                    Log.d(TAG, "고위험 URL 자동 차단됨: $url")
                }

                // 6. 위험도에 따라 푸시 알림 표시 (설정에서 활성화된 경우만)
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                val isPushEnabled = settings?.isPushNotificationEnabled() ?: true

                when (riskLevel) {
                    RiskLevel.DANGER -> {
                        if (isPushEnabled) {
                            showSecurityAlert(
                                title = "⚠️ 위험한 링크 감지!",
                                message = "[$title]에서 위험한 URL이 발견되었습니다.\n위험도: $riskScore%",
                                url = url,
                                notificationId = notificationId,
                                level = "DANGER"
                            )
                        }
                    }
                    RiskLevel.CAUTION -> {
                        if (isPushEnabled) {
                            showSecurityAlert(
                                title = "⚡ 주의가 필요한 링크",
                                message = "[$title]에서 의심스러운 URL이 발견되었습니다.\n위험도: $riskScore%",
                                url = url,
                                notificationId = notificationId,
                                level = "CAUTION"
                            )
                        }
                    }
                    else -> {
<<<<<<< HEAD
                        Log.d(TAG, "✅ 안전한 URL: $url (점수: $riskScore%)")
=======
                        Log.d(TAG, "안전한 URL: $url (점수: $riskScore%)")
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    }
                }

            } catch (e: Exception) {
<<<<<<< HEAD
                Log.e(TAG, "❌ URL 분석/저장 실패: ${e.message}", e)
=======
                Log.e(TAG, "URL 분석/저장 실패: ${e.message}", e)
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
            }
        }
    }

    /**
<<<<<<< HEAD
     * 🆕 AI 응답에서 상세 설명 텍스트 생성
     */
    private fun buildAiExplanation(aiResponse: com.mop.phishing.data.remote.GetAIAnalysisResponse): String? {
        val parts = mutableListOf<String>()

        aiResponse.riskSummary?.let {
            parts.add("📌 $it")
        }

        aiResponse.phishingType?.let { type ->
            if (type != "정상 사이트") {
                parts.add("🎯 피싱 유형: $type")
            }
        }

        aiResponse.targetedBrand?.let { brand ->
            parts.add("⚠️ 사칭 대상: $brand")
        }

        if (!aiResponse.phishingTechniques.isNullOrEmpty()) {
            parts.add("🔧 피싱 기법: ${aiResponse.phishingTechniques.joinToString(", ")}")
        }

        aiResponse.technicalAnalysis?.let {
            parts.add("🔍 기술 분석: $it")
        }

        aiResponse.attackScenario?.let {
            parts.add("⚡ 공격 시나리오: $it")
        }

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
     * 기본 권장사항 반환
     */
    private fun getDefaultRecommendation(riskLevel: RiskLevel): String {
        return when (riskLevel) {
            RiskLevel.SAFE -> "안전한 링크입니다. 정상적으로 접속하셔도 됩니다."
            RiskLevel.CAUTION -> "주의가 필요한 링크입니다. 개인정보 입력에 주의하세요."
            RiskLevel.DANGER -> "위험한 링크입니다. 접속을 권장하지 않습니다."
            RiskLevel.UNKNOWN -> "분석 중입니다. 잠시만 기다려주세요."
        }
    }

    /**
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
     * 보안 알림 표시
     */
    private fun showSecurityAlert(
        title: String,
        message: String,
        url: String,
        notificationId: String,
        level: String
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val settings = settingsRepository

<<<<<<< HEAD
        val isSoundEnabled = settings?.isSoundNotificationEnabled() ?: true
        val isVibrationEnabled = settings?.isVibrationEnabled() ?: true

=======
        // 설정 값 읽기
        val isSoundEnabled = settings?.isSoundNotificationEnabled() ?: true
        val isVibrationEnabled = settings?.isVibrationEnabled() ?: true

        // 클릭 시 앱 열기 + 해당 알림 상세 페이지로 이동
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        val intent = Intent(this, MainActivityCompose::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_url", url)
            putExtra("alert_level", level)
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            url.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = when (level) {
            "DANGER" -> android.R.drawable.ic_dialog_alert
            "CAUTION" -> android.R.drawable.ic_dialog_info
            else -> android.R.drawable.ic_dialog_info
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nURL: $url"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

<<<<<<< HEAD
=======
        // 소리 설정 적용
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        if (isSoundEnabled) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }

<<<<<<< HEAD
        if (isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
=======
        // 진동 설정 적용
        if (isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
            // 직접 진동도 실행 (알림과 별도로)
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
            triggerVibration()
        }

        notificationManager.notify(url.hashCode(), builder.build())
<<<<<<< HEAD
        Log.d(TAG, "📢 보안 알림 표시: $title")
    }

=======
        Log.d(TAG, "보안 알림 표시: $title (소리=$isSoundEnabled, 진동=$isVibrationEnabled)")
    }

    /**
     * 진동 실행
     */
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "진동 실행 실패: ${e.message}")
        }
    }

<<<<<<< HEAD
=======
    /**
     * 알림 채널 생성 (Android 8.0+)
     */
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "악성 URL 탐지 알림"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
