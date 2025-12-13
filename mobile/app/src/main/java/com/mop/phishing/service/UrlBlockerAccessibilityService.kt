package com.mop.phishing.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import com.mop.phishing.R
import com.mop.phishing.util.UrlBlockManager

/**
 * URL 차단 접근성 서비스
 * 브라우저에서 차단된 URL 접근 시 경고 오버레이 표시
 */
class UrlBlockerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "UrlBlockerService"
        
        // 지원하는 브라우저 패키지명
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "com.sec.android.app.sbrowser",      // 삼성 인터넷
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.brave.browser",
            "com.microsoft.emmx",                // Edge
            "com.naver.whale",                   // 네이버 웨일
            "com.ksmobile.cb",                   // CM Browser
            "com.UCMobile.intl",                 // UC Browser
            "com.android.browser",               // 기본 브라우저
        )
        
        // 서비스 상태
        var isRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lastBlockedUrl: String? = null
    private var lastBlockTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "URL 차단 서비스 연결됨")
        isRunning = true

        // 서비스 설정
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                   AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        serviceInfo = info

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 브라우저 앱인지 확인
        if (packageName !in BROWSER_PACKAGES) return

        // URL 추출 시도
        try {
            val rootNode = rootInActiveWindow ?: return
            val url = extractUrlFromNode(rootNode)
            // rootNode는 recycle하지 않음 (시스템이 관리)
            
            if (!url.isNullOrBlank()) {
                checkAndBlockUrl(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "이벤트 처리 오류: ${e.message}")
        }
    }

    /**
     * 노드에서 URL 추출 (recycle 호출 제거)
     */
    private fun extractUrlFromNode(node: AccessibilityNodeInfo): String? {
        try {
            // 주소창 찾기 (다양한 브라우저 지원)
            val urlBarIds = listOf(
                "com.android.chrome:id/url_bar",
                "com.android.chrome:id/omnibox_url_field",
                "com.sec.android.app.sbrowser:id/location_bar_edit_text",
                "org.mozilla.firefox:id/url_bar_title",
                "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
                "com.opera.browser:id/url_field",
                "com.naver.whale:id/url_bar",
                "com.brave.browser:id/url_bar",
                "com.microsoft.emmx:id/url_bar"
            )

            for (urlBarId in urlBarIds) {
                val nodes = node.findAccessibilityNodeInfosByViewId(urlBarId)
                if (nodes != null && nodes.isNotEmpty()) {
                    val urlNode = nodes[0]
                    val url = urlNode.text?.toString()
                    // nodes는 시스템이 관리하므로 recycle하지 않음
                    if (!url.isNullOrBlank()) {
                        return normalizeUrl(url)
                    }
                }
            }

            // EditText에서 URL 패턴 찾기 (폴백)
            return findUrlInEditText(node)
        } catch (e: Exception) {
            Log.e(TAG, "URL 추출 오류: ${e.message}")
            return null
        }
    }

    /**
     * EditText에서 URL 찾기 (recycle 호출 제거)
     */
    private fun findUrlInEditText(node: AccessibilityNodeInfo): String? {
        try {
            if (node.className?.toString() == "android.widget.EditText") {
                val text = node.text?.toString()
                if (text != null && (text.contains("http") || text.contains("www") || text.contains("."))) {
                    return normalizeUrl(text)
                }
            }

            // 자식 노드 탐색
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val url = findUrlInEditText(child)
                // child는 recycle하지 않음
                if (url != null) return url
            }
        } catch (e: Exception) {
            Log.e(TAG, "EditText URL 검색 오류: ${e.message}")
        }

        return null
    }

    /**
     * URL 정규화
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }
        return normalized
    }

    /**
     * URL 차단 확인 및 오버레이 표시
     */
    private fun checkAndBlockUrl(url: String) {
        // 중복 차단 방지 (같은 URL, 3초 이내)
        val now = System.currentTimeMillis()
        if (url == lastBlockedUrl && now - lastBlockTime < 3000) {
            return
        }

        // 차단 목록 확인
        if (UrlBlockManager.isBlocked(this, url)) {
            Log.d(TAG, "차단된 URL 감지: $url")
            lastBlockedUrl = url
            lastBlockTime = now
            
            handler.post {
                showBlockOverlay(url)
            }
        }
    }

    /**
     * 차단 오버레이 표시
     */
    private fun showBlockOverlay(url: String) {
        // 이미 표시 중이면 제거
        removeOverlay()

        // 오버레이 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "오버레이 권한 없음")
            return
        }

        try {
            val inflater = LayoutInflater.from(this)
            overlayView = inflater.inflate(R.layout.overlay_url_blocked, null)

            // URL 표시
            overlayView?.findViewById<TextView>(R.id.tvBlockedUrl)?.text = url

            // 도메인 추출해서 표시
            val domain = UrlBlockManager.extractDomain(url)
            overlayView?.findViewById<TextView>(R.id.tvBlockedDomain)?.text = domain

            // 뒤로가기 버튼
            overlayView?.findViewById<Button>(R.id.btnGoBack)?.setOnClickListener {
                removeOverlay()
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            // 위험 감수 버튼
            overlayView?.findViewById<Button>(R.id.btnProceed)?.setOnClickListener {
                removeOverlay()
                // 일시적으로 허용 (이 세션에서만)
                lastBlockedUrl = null
            }

            // 윈도우 파라미터 설정
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager?.addView(overlayView, params)
            Log.d(TAG, "차단 오버레이 표시됨")

        } catch (e: Exception) {
            Log.e(TAG, "오버레이 표시 실패: ${e.message}", e)
        }
    }

    /**
     * 오버레이 제거
     */
    private fun removeOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "오버레이 제거 실패: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "서비스 중단됨")
        removeOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "서비스 종료됨")
        isRunning = false
        removeOverlay()
    }
}
