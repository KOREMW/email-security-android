package com.mop.phishing.util

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
        }
    }
    
    /**
     * URL에서 도메인 추출
     */
    fun extractDomain(url: String): String {
        return try {
            Uri.parse(url).host ?: url
        } catch (e: Exception) {
            url
        }
    }
}
