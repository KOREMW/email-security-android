package com.mop.phishing.util

import android.content.Context
import android.content.SharedPreferences

/**
 * URL 차단 관리자
 * SharedPreferences를 사용하여 차단된 URL 목록 관리
 */
object UrlBlockManager {

    private const val PREFS_NAME = "url_block_prefs"
    private const val KEY_BLOCKED_URLS = "blocked_urls"
    private const val KEY_BLOCKED_DOMAINS = "blocked_domains"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * URL 차단
     */
    fun blockUrl(context: Context, url: String) {
        val blockedUrls = getBlockedUrls(context).toMutableSet()
        blockedUrls.add(normalizeUrl(url))
        saveBlockedUrls(context, blockedUrls)
    }

    /**
     * URL 차단 해제
     */
    fun unblockUrl(context: Context, url: String) {
        val blockedUrls = getBlockedUrls(context).toMutableSet()
        blockedUrls.remove(normalizeUrl(url))
        saveBlockedUrls(context, blockedUrls)
    }

    /**
     * URL이 차단되어 있는지 확인
     */
    fun isBlocked(context: Context, url: String): Boolean {
        val normalizedUrl = normalizeUrl(url)
        val blockedUrls = getBlockedUrls(context)
        
        // 정확한 매칭
        if (blockedUrls.contains(normalizedUrl)) {
            return true
        }
        
        // 도메인 기반 매칭 (옵션)
        val domain = extractDomain(url)
        return blockedUrls.any { blockedUrl ->
            extractDomain(blockedUrl) == domain
        }
    }

    /**
     * 차단된 URL 목록 가져오기
     */
    fun getBlockedUrls(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_BLOCKED_URLS, emptySet()) ?: emptySet()
    }

    /**
     * 차단된 URL 목록 저장
     */
    private fun saveBlockedUrls(context: Context, urls: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_BLOCKED_URLS, urls).apply()
    }

    /**
     * 도메인 차단
     */
    fun blockDomain(context: Context, domain: String) {
        val blockedDomains = getBlockedDomains(context).toMutableSet()
        blockedDomains.add(domain.lowercase().trim())
        saveBlockedDomains(context, blockedDomains)
    }

    /**
     * 도메인 차단 해제
     */
    fun unblockDomain(context: Context, domain: String) {
        val blockedDomains = getBlockedDomains(context).toMutableSet()
        blockedDomains.remove(domain.lowercase().trim())
        saveBlockedDomains(context, blockedDomains)
    }

    /**
     * 차단된 도메인 목록 가져오기
     */
    fun getBlockedDomains(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_BLOCKED_DOMAINS, emptySet()) ?: emptySet()
    }

    /**
     * 차단된 도메인 목록 저장
     */
    private fun saveBlockedDomains(context: Context, domains: Set<String>) {
        getPrefs(context).edit().putStringSet(KEY_BLOCKED_DOMAINS, domains).apply()
    }

    /**
     * 모든 차단 해제
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_BLOCKED_URLS)
            .remove(KEY_BLOCKED_DOMAINS)
            .apply()
    }

    /**
     * URL 정규화 (비교를 위해)
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim().lowercase()
        
        // 프로토콜 제거
        normalized = normalized.removePrefix("https://").removePrefix("http://")
        
        // www 제거
        normalized = normalized.removePrefix("www.")
        
        // 끝의 슬래시 제거
        normalized = normalized.trimEnd('/')
        
        return normalized
    }

    /**
     * URL에서 도메인 추출 (public - 접근성 서비스에서 사용)
     */
    fun extractDomain(url: String): String {
        val normalized = normalizeUrl(url)
        return normalized.split("/").firstOrNull() ?: normalized
    }
}
