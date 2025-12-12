package com.mop.phishing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mop.phishing.data.remote.CloudFunctionRepository
import com.mop.phishing.data.remote.ResponseMapper
import com.mop.phishing.ml.UrlClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 알림 리스너 서비스
 * 메일/메신저 알림에서 URL을 추출하고 분석
 */
class MailNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotiListener"
        private const val CHANNEL_ID = "security_alert"
        private const val CHANNEL_NAME = "보안 알림"
        
        // 감시할 앱 패키지 목록
        private val MONITORED_PACKAGES = listOf(
            "com.google.android.gm",           // Gmail
            "com.samsung.android.email.provider", // Samsung Email
            "com.kakao.talk",                  // KakaoTalk
            "com.android.mms",                 // 기본 문자
            "com.samsung.android.messaging"    // Samsung 문자
        )

        // 위험 임계값
        private const val DANGER_THRESHOLD = 0.7f
        private const val CAUTION_THRESHOLD = 0.4f
    }

    private var classifier: UrlClassifier? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "알림 리스너 연결됨")
        
        try {
            classifier = UrlClassifier(this)
            Log.d(TAG, "UrlClassifier 초기화 완료")
        } catch (e: Exception) {
            Log.e(TAG, "UrlClassifier 초기화 실패: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "알림 리스너 연결 해제됨")
        classifier?.close()
        classifier = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        
        // 모니터링 대상 앱인지 확인
        if (!isMonitoredPackage(pkg)) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullContent = "$title $text $bigText"
        
        Log.d(TAG, "알림 수신: pkg=$pkg, title=$title")

        // URL 추출
        val urls = extractUrls(fullContent)
        if (urls.isEmpty()) {
            Log.d(TAG, "URL 없음, 건너뜀")
            return
        }

        Log.d(TAG, "발견된 URL: ${urls.size}개")

        // 각 URL 분석
        urls.forEach { url ->
            analyzeUrl(url, pkg, title)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    /**
     * 모니터링 대상 앱인지 확인
     */
    private fun isMonitoredPackage(pkg: String): Boolean {
        return MONITORED_PACKAGES.any { pkg.contains(it) || it.contains(pkg) }
    }

    /**
     * URL 추출 (정규식)
     */
    private fun extractUrls(text: String): List<String> {
        val regex = Regex("(https?://[\\w./?=&%\\-#@:]+)")
        return regex.findAll(text)
            .map { it.value }
            .distinct()
            .toList()
    }

    /**
     * URL 분석 실행
     */
    private fun analyzeUrl(url: String, sourcePackage: String, title: String) {
        serviceScope.launch {
            try {
                // 1. 로컬 ML 분석
                val localScore = classifier?.predict(url) ?: 0.5f
                Log.d(TAG, "URL=$url, 로컬 점수=$localScore")

                // 2. 서버 분석 요청
                val response = CloudFunctionRepository.analyzeSuspiciousUrl(
                    url = url,
                    source = "notification",
                    localRiskScore = localScore.toDouble()
                )

                Log.d(TAG, "서버 응답: suspicious=${response.suspicious}, score=${response.finalRiskScore}")

                // 3. 위험도에 따라 알림 표시
                val riskLevel = ResponseMapper.mapSuspiciousLevelToRiskLevel(response.suspiciousLevel)
                val riskScore = ResponseMapper.mapScoreToPercent(response.finalRiskScore)

                when {
                    response.finalRiskScore >= DANGER_THRESHOLD -> {
                        showSecurityAlert(
                            title = "⚠️ 위험한 링크 감지!",
                            message = "[$title]에서 위험한 URL이 발견되었습니다.\n위험도: $riskScore%",
                            url = url,
                            level = "DANGER"
                        )
                    }
                    response.finalRiskScore >= CAUTION_THRESHOLD -> {
                        showSecurityAlert(
                            title = "⚡ 주의가 필요한 링크",
                            message = "[$title]에서 의심스러운 URL이 발견되었습니다.\n위험도: $riskScore%",
                            url = url,
                            level = "CAUTION"
                        )
                    }
                    else -> {
                        Log.d(TAG, "안전한 URL: $url (점수: ${response.finalRiskScore})")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "URL 분석 실패: ${e.message}", e)
                
                // 서버 연결 실패 시 로컬 점수로 판단
                val localScore = classifier?.predict(url) ?: 0.5f
                if (localScore >= DANGER_THRESHOLD) {
                    showSecurityAlert(
                        title = "⚠️ 의심스러운 링크 감지",
                        message = "[$title]에서 의심스러운 URL이 발견되었습니다.\n(오프라인 분석)",
                        url = url,
                        level = "CAUTION"
                    )
                }
            }
        }
    }

    /**
     * 보안 알림 표시
     */
    private fun showSecurityAlert(
        title: String,
        message: String,
        url: String,
        level: String
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // 클릭 시 앱 열기
        val intent = Intent(this, MainActivityCompose::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alert_url", url)
            putExtra("alert_level", level)
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

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nURL: $url"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(url.hashCode(), notification)
        Log.d(TAG, "보안 알림 표시: $title")
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
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
