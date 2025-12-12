package com.mop.phishing.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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

object AccessibilityUtils {
    fun openAccessibilitySettings(context: Context) { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
    fun openNotificationListenerSettings(context: Context) { context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
    fun openAppSettings(context: Context) { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null); flags = Intent.FLAG_ACTIVITY_NEW_TASK }) }
}
