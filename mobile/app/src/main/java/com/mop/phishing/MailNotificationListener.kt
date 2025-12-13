package com.mop.phishing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.AudioAttributes
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
 * 분석 결과를 Repository에 저장하여 UI에 표시
 */
class MailNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotiListener"
        private const val CHANNEL_ID = "security_alert"
        private const val CHANNEL_NAME = "보안 알림"
        
        // 감시할 앱 패키지 목록
        private val MONITORED_PACKAGES = mapOf(
            "com.google.android.gm" to NotificationSource.GMAIL,
            "com.samsung.android.email.provider" to NotificationSource.GMAIL,
            "com.kakao.talk" to NotificationSource.KAKAOTALK,
            "com.android.mms" to NotificationSource.SMS,
            "com.samsung.android.messaging" to NotificationSource.SMS
        )

        // 위험 임계값
        private const val DANGER_THRESHOLD = 0.7f
        private const val CAUTION_THRESHOLD = 0.4f
    }

    private var classifier: UrlClassifier? = null
    private var repository: NotificationRepository? = null
    private var settingsRepository: SettingsRepository? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Repository 초기화
        repository = NotificationRepository.getInstance(this)
        settingsRepository = SettingsRepository.getInstance(this)
        Log.d(TAG, "Repository 초기화 완료")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "알림 리스너 연결됨")

        try {
            // 싱글톤 사용으로 메모리 효율 개선
            classifier = UrlClassifier.getInstance(this)
            Log.d(TAG, "UrlClassifier 싱글톤 연결 완료")
        } catch (e: Exception) {
            Log.e(TAG, "UrlClassifier 초기화 실패: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "알림 리스너 연결 해제됨")
        // 싱글톤이므로 close하지 않음 (다른 곳에서 사용 중일 수 있음)
        classifier = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        
        // 모니터링 대상 앱인지 확인 및 소스 타입 가져오기
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

        // URL 추출
        val urls = extractUrls(fullContent)
        if (urls.isEmpty()) {
            Log.d(TAG, "URL 없음, 건너뜀")
            return
        }

        Log.d(TAG, "발견된 URL: ${urls.size}개")

        // 각 URL 분석
        urls.forEach { url ->
            analyzeAndSaveUrl(
                url = url,
                source = notificationSource,
                sourcePackage = pkg,
                title = title,
                content = fullContent
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    /**
     * 패키지명으로 NotificationSource 반환
     */
    private fun getNotificationSource(pkg: String): NotificationSource? {
        for ((key, source) in MONITORED_PACKAGES) {
            if (pkg.contains(key) || key.contains(pkg)) {
                return source
            }
        }
        return null
    }

    /**
     * URL 추출 (정규식)
     * 개선된 정규식: 한글, 틸드, 플러스 등 더 많은 문자 지원
     */
    private fun extractUrls(text: String): List<String> {
        // 기존: (https?://[\w./?=&%\-#@:]+)
        // 개선: 틸드(~), 플러스(+), 세미콜론(;), 콤마(,), 느낌표(!), 별표(*),
        //       작은따옴표('), 괄호(()), 대괄호([]), 유니코드 문자 지원
        val regex = Regex(
            "(https?://[\\w./?=&%\\-#@:~+;,!*'()\\[\\]\\u0080-\\uFFFF]+)",
            RegexOption.IGNORE_CASE
        )
        return regex.findAll(text)
            .map { it.value.trimEnd('.', ',', ')', ']', '\'', '!') } // 문장 끝 부호 제거
            .filter { isValidUrl(it) }
            .distinct()
            .toList()
    }

    /**
     * URL 유효성 검사
     */
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.host != null && uri.host.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * URL 분석 및 Repository 저장
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

                // 1. Repository에 알림 저장 (미분석 상태)
                val notificationId = repo.saveNotification(
                    source = source,
                    title = title,
                    content = content,
                    url = url,
                    sourcePackage = sourcePackage
                )
                Log.d(TAG, "알림 저장 완료: id=$notificationId")

                // 2. 로컬 ML 분석
                val localScore = classifier?.predict(url) ?: 0.5f
                Log.d(TAG, "URL=$url, 로컬 점수=$localScore")

                // 3. 서버 분석 요청
                var riskLevel = RiskLevel.UNKNOWN
                var riskScore = (localScore * 100).toInt()
                var vtMalicious = 0
                var vtSuspicious = 0
                var aiExplanation: String? = null
                
                try {
                    val response = CloudFunctionRepository.analyzeSuspiciousUrl(
                        url = url,
                        source = "notification",
                        localRiskScore = localScore.toDouble()
                    )

                    Log.d(TAG, "서버 응답: suspicious=${response.suspicious}, score=${response.finalRiskScore}")

                    // 서버 응답으로 업데이트
                    riskLevel = ResponseMapper.mapSuspiciousLevelToRiskLevel(response.suspiciousLevel)
                    riskScore = ResponseMapper.mapScoreToPercent(response.finalRiskScore)
                    vtMalicious = response.virusTotal?.malicious ?: 0
                    vtSuspicious = response.virusTotal?.suspicious ?: 0
                    
                } catch (serverError: Exception) {
                    Log.w(TAG, "서버 분석 실패, 로컬 점수로 판단: ${serverError.message}")
                    
                    // 서버 실패 시 로컬 점수로 위험도 결정
                    riskLevel = when {
                        localScore >= DANGER_THRESHOLD -> RiskLevel.DANGER
                        localScore >= CAUTION_THRESHOLD -> RiskLevel.CAUTION
                        else -> RiskLevel.SAFE
                    }
                    riskScore = (localScore * 100).toInt()
                }

                // 4. 분석 결과 업데이트
                repo.updateAnalysisResult(
                    id = notificationId,
                    riskLevel = riskLevel,
                    riskScore = riskScore,
                    localMlScore = localScore,
                    vtMalicious = vtMalicious,
                    vtSuspicious = vtSuspicious,
                    aiExplanation = aiExplanation
                )
                Log.d(TAG, "분석 결과 저장 완료: $riskLevel ($riskScore%)")

                // 5. 고위험 자동 차단 설정 확인
                if (riskLevel == RiskLevel.DANGER && settings?.isAutoBlockHighRiskEnabled() == true) {
                    UrlBlockManager.blockUrl(this@MailNotificationListener, url)
                    Log.d(TAG, "고위험 URL 자동 차단됨: $url")
                }

                // 6. 위험도에 따라 푸시 알림 표시 (설정에서 활성화된 경우만)
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
                        Log.d(TAG, "안전한 URL: $url (점수: $riskScore%)")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "URL 분석/저장 실패: ${e.message}", e)
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
        notificationId: String,
        level: String
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val settings = settingsRepository

        // 설정 값 읽기
        val isSoundEnabled = settings?.isSoundNotificationEnabled() ?: true
        val isVibrationEnabled = settings?.isVibrationEnabled() ?: true

        // 클릭 시 앱 열기 + 해당 알림 상세 페이지로 이동
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

        // 소리 설정 적용
        if (isSoundEnabled) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }

        // 진동 설정 적용
        if (isVibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
            // 직접 진동도 실행 (알림과 별도로)
            triggerVibration()
        }

        notificationManager.notify(url.hashCode(), builder.build())
        Log.d(TAG, "보안 알림 표시: $title (소리=$isSoundEnabled, 진동=$isVibrationEnabled)")
    }

    /**
     * 진동 실행
     */
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
