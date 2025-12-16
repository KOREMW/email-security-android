package com.mop.phishing.util

<<<<<<< HEAD
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.mop.phishing.ui.components.RiskLevel
import com.mop.phishing.ui.screens.detail.AnalysisResult

/**
 * URL 관련 액션 유틸리티
 * 공유, 신고, 브라우저 열기 등
 */
object UrlActionUtils {
    
    /**
     * 분석 결과 공유
     */
    fun shareAnalysisResult(context: Context, url: String, result: AnalysisResult) {
        val riskText = when (result.riskLevel) {
            RiskLevel.SAFE -> "✅ 안전"
            RiskLevel.CAUTION -> "⚠️ 주의"
            RiskLevel.DANGER -> "🚨 위험"
            RiskLevel.UNKNOWN -> "❓ 미분석"
        }
        
        val shareText = buildString {
            append("🔒 URL 보안 검사 결과\n\n")
            append("📎 URL: $url\n")
            append("📊 위험도: $riskText (${result.riskScore}점)\n\n")
            append("💡 권장사항:\n${result.recommendation}\n\n")
            append("━━━━━━━━━━━━━━━\n")
            append("피싱 탐지 앱으로 검사됨")
        }
        
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "검사 결과 공유"))
    }

    /**
     * URL만 공유
     */
    fun shareUrl(context: Context, url: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "URL 공유"))
    }

    /**
     * Google Safe Browsing에 URL 신고
     */
    fun reportUrl(context: Context, url: String) {
        try {
            val reportUrl = "https://safebrowsing.google.com/safebrowsing/report_phish/?url=${Uri.encode(url)}"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl)))
        } catch (e: Exception) {
            Toast.makeText(context, "브라우저를 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 브라우저에서 URL 열기
     */
    fun openUrlInBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "URL을 열 수 없습니다", Toast.LENGTH_SHORT).show()
=======
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.mop.phishing.service.UrlBlockerAccessibilityService
import com.mop.phishing.ui.components.RiskLevel
import com.mop.phishing.ui.screens.detail.AnalysisResult

object UrlActionUtils {
    fun shareAnalysisResult(context: Context, url: String, result: AnalysisResult) {
        val riskText = when (result.riskLevel) {
            RiskLevel.SAFE -> "✅ 안전"; RiskLevel.CAUTION -> "⚠️ 주의"; RiskLevel.DANGER -> "🚨 위험"; RiskLevel.UNKNOWN -> "❓ 미분석"
        }
        val shareText = "🔒 URL 보안 검사 결과\n\n📎 URL: $url\n📊 위험도: $riskText (${result.riskScore}점)\n\n💡 권장사항:\n${result.recommendation}\n\n━━━━━━━━━━━━━━━\n피싱 탐지 앱으로 검사됨"
        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }
        context.startActivity(Intent.createChooser(sendIntent, "검사 결과 공유"))
    }

    fun shareUrl(context: Context, url: String) {
        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, url); type = "text/plain" }
        context.startActivity(Intent.createChooser(sendIntent, "URL 공유"))
    }

    fun reportUrl(context: Context, url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://safebrowsing.google.com/safebrowsing/report_phish/?url=${Uri.encode(url)}"))) }
        catch (e: Exception) { Toast.makeText(context, "브라우저를 열 수 없습니다", Toast.LENGTH_SHORT).show() }
    }

    fun openUrlInBrowser(context: Context, url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { Toast.makeText(context, "URL을 열 수 없습니다", Toast.LENGTH_SHORT).show() }
    }
}

object UrlBlockManager {
    private const val PREFS_NAME = "blocked_urls"
    private const val KEY_BLOCKED_LIST = "blocked_list"
    private const val KEY_BLOCKED_DOMAINS = "blocked_domains"

    fun blockUrl(context: Context, url: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet(KEY_BLOCKED_LIST, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        blockedSet.add(url)
        return prefs.edit().putStringSet(KEY_BLOCKED_LIST, blockedSet).commit()
    }

    fun blockDomain(context: Context, domain: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet(KEY_BLOCKED_DOMAINS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        blockedSet.add(domain)
        return prefs.edit().putStringSet(KEY_BLOCKED_DOMAINS, blockedSet).commit()
    }

    fun unblockUrl(context: Context, url: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet(KEY_BLOCKED_LIST, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        blockedSet.remove(url)
        return prefs.edit().putStringSet(KEY_BLOCKED_LIST, blockedSet).commit()
    }

    fun unblockDomain(context: Context, domain: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blockedSet = prefs.getStringSet(KEY_BLOCKED_DOMAINS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        blockedSet.remove(domain)
        return prefs.edit().putStringSet(KEY_BLOCKED_DOMAINS, blockedSet).commit()
    }

    fun isBlocked(context: Context, url: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blockedUrls = prefs.getStringSet(KEY_BLOCKED_LIST, emptySet()) ?: emptySet()
        if (blockedUrls.contains(url)) return true
        val blockedDomains = prefs.getStringSet(KEY_BLOCKED_DOMAINS, emptySet()) ?: emptySet()
        val domain = extractDomain(url)
        return blockedDomains.any { domain.contains(it) || it.contains(domain) }
    }

    fun getBlockedUrls(context: Context): Set<String> = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getStringSet(KEY_BLOCKED_LIST, emptySet()) ?: emptySet()
    fun getBlockedDomains(context: Context): Set<String> = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getStringSet(KEY_BLOCKED_DOMAINS, emptySet()) ?: emptySet()
    fun clearAll(context: Context): Boolean = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    fun extractDomain(url: String): String = try { Uri.parse(url).host ?: url } catch (e: Exception) { url }
    fun getBlockStats(context: Context): BlockStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val blockedUrls = prefs.getStringSet(KEY_BLOCKED_LIST, emptySet())?.size ?: 0
        val blockedDomains = prefs.getStringSet(KEY_BLOCKED_DOMAINS, emptySet())?.size ?: 0
        return BlockStats(blockedUrls, blockedDomains, blockedUrls + blockedDomains)
    }
}

data class BlockStats(val blockedUrlCount: Int, val blockedDomainCount: Int, val totalBlocked: Int)

/**
 * 접근성 및 권한 관련 유틸리티
 */
object AccessibilityUtils {
    
    /**
     * 접근성 서비스 활성화 여부 확인
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_GENERIC
        )
        
        val serviceName = "${context.packageName}/${UrlBlockerAccessibilityService::class.java.canonicalName}"
        
        return enabledServices.any { 
            it.id == serviceName || 
            it.resolveInfo?.serviceInfo?.name == UrlBlockerAccessibilityService::class.java.canonicalName
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
        }
    }
    
    /**
<<<<<<< HEAD
     * URL에서 도메인 추출
     */
    fun extractDomain(url: String): String {
        return try {
            Uri.parse(url).host ?: url
        } catch (e: Exception) {
            url
        }
    }
=======
     * 오버레이 권한 확인
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    /**
     * URL 차단 기능이 완전히 활성화되었는지 확인
     * (접근성 서비스 + 오버레이 권한 모두 필요)
     */
    fun isUrlBlockingFullyEnabled(context: Context): Boolean {
        return isAccessibilityServiceEnabled(context) && canDrawOverlays(context)
    }
    
    /**
     * 접근성 설정 화면 열기
     */
    fun openAccessibilitySettings(context: Context) { 
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { 
                flags = Intent.FLAG_ACTIVITY_NEW_TASK 
            }
        ) 
    }
    
    /**
     * 오버레이 권한 설정 화면 열기
     */
    fun openOverlayPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
    }
    
    /**
     * 알림 리스너 설정 화면 열기
     */
    fun openNotificationListenerSettings(context: Context) { 
        context.startActivity(
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply { 
                flags = Intent.FLAG_ACTIVITY_NEW_TASK 
            }
        ) 
    }
    
    /**
     * 앱 설정 화면 열기
     */
    fun openAppSettings(context: Context) { 
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { 
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK 
            }
        ) 
    }
    
    /**
     * 권한 상태 데이터 클래스
     */
    data class PermissionStatus(
        val accessibilityEnabled: Boolean,
        val overlayEnabled: Boolean,
        val notificationListenerEnabled: Boolean
    ) {
        val isFullyEnabled: Boolean
            get() = accessibilityEnabled && overlayEnabled
    }
    
    /**
     * 모든 권한 상태 확인
     */
    fun checkAllPermissions(context: Context): PermissionStatus {
        return PermissionStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            overlayEnabled = canDrawOverlays(context),
            notificationListenerEnabled = isNotificationListenerEnabled(context)
        )
    }
    
    /**
     * 알림 리스너 권한 확인
     */
    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(context.packageName) == true
    }
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
}
