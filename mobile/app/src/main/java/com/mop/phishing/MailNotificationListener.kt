package com.mop.phishing  // 네 패키지로 변경

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.mop.phishing.ml.UrlClassifier

class MailNotificationListener : NotificationListenerService() {

    private var classifier: UrlClassifier? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotiListener", "Connected")
        // Service도 Context이므로 그대로 넘겨서 초기화
        classifier = UrlClassifier(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        classifier?.close()
        classifier = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        Log.d("NotiListener", "posted from pkg=$pkg, title=$title, text=$text")

        // 1) 메일/메신저 앱만 필터링 (예: Gmail)
        if (!pkg.contains("gmail") && !pkg.contains("mail")) {
            return
        }

        // 2) 본문에서 URL 추출 (정규식)
        val urls = extractUrls("$title $text")
        if (urls.isEmpty()) return

        // 3) 각 URL을 모델로 평가
        val c = classifier ?: return
        for (url in urls) {
            val prob = c.predict(url)
            Log.d("NotiListener", "URL=$url, 피싱 확률=$prob")
            if (prob >= 0.7f) {
                // TODO: 여기서 사용자에게 별도 알림/토스트 띄우기
                Log.d("NotiListener", "위험 URL 의심, 알림 필요")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    private fun extractUrls(text: String): List<String> {
        val regex = Regex("(https?://[\\w./?=&%\\-]+)")
        return regex.findAll(text).map { it.value }.toList()
    }
}
