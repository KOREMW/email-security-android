package com.mop.phishing.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mop.phishing.ui.components.NotificationItem
import com.mop.phishing.ui.components.NotificationSource
import com.mop.phishing.ui.components.RiskLevel

/**
 * 알림 데이터 엔티티
 * Room Database에 저장되는 알림 정보
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    val id: String,
    
    // 알림 출처 (GMAIL, SMS, KAKAOTALK 등)
    val source: String,
    
    // 알림 제목
    val title: String,
    
    // 알림 내용
    val content: String,
    
    // 추출된 URL
    val url: String,
    
    // 수신 시간 (timestamp)
    val receivedAt: Long = System.currentTimeMillis(),
    
    // 분석 완료 여부
    val isAnalyzed: Boolean = false,
    
    // 위험도 레벨 (SAFE, CAUTION, DANGER, UNKNOWN)
    val riskLevel: String = "UNKNOWN",
    
    // 위험도 점수 (0-100)
    val riskScore: Int = 0,
    
    // 로컬 ML 점수 (0.0-1.0)
    val localMlScore: Float = 0f,
    
    // VirusTotal 악성 탐지 수
    val virusTotalMalicious: Int = 0,
    
    // VirusTotal 의심 탐지 수
    val virusTotalSuspicious: Int = 0,
    
    // VirusTotal 전체 엔진 수
    val virusTotalTotal: Int = 0,
    
    // 최종 위험도 점수 (앙상블)
    val finalScore: Int = 0,
    
    // AI 분석 설명
    val aiExplanation: String? = null,
    
    // 권장 사항
    val recommendation: String? = null,
    
    // 접속 가능 여부
    val canAccess: Boolean = true,
    
    // 앱 패키지명 (출처 앱)
    val sourcePackage: String? = null
)

/**
 * Entity → UI Model 변환
 */
fun NotificationEntity.toNotificationItem(): NotificationItem {
    return NotificationItem(
        id = this.id,
        source = NotificationSource.valueOf(this.source),
        title = this.title,
        content = this.content,
        url = this.url,
        timeAgo = formatTimeAgo(this.receivedAt),
        riskLevel = RiskLevel.valueOf(this.riskLevel),
        riskScore = this.riskScore,
        isAnalyzed = this.isAnalyzed,
        // 상세 분석 결과도 포함
        localMlScore = this.localMlScore,
        virusTotalMalicious = this.virusTotalMalicious,
        virusTotalSuspicious = this.virusTotalSuspicious,
        virusTotalTotal = this.virusTotalTotal,
        finalScore = this.finalScore,
        aiExplanation = this.aiExplanation,
        recommendation = this.recommendation,
        canAccess = this.canAccess
    )
}

/**
 * UI Model → Entity 변환
 */
fun NotificationItem.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = this.id,
        source = this.source.name,
        title = this.title,
        content = this.content,
        url = this.url,
        riskLevel = this.riskLevel.name,
        riskScore = this.riskScore,
        isAnalyzed = this.isAnalyzed,
        localMlScore = this.localMlScore,
        virusTotalMalicious = this.virusTotalMalicious,
        virusTotalSuspicious = this.virusTotalSuspicious,
        virusTotalTotal = this.virusTotalTotal,
        finalScore = this.finalScore,
        aiExplanation = this.aiExplanation,
        recommendation = this.recommendation,
        canAccess = this.canAccess
    )
}

/**
 * 시간 포맷팅 (방금 전, 5분 전, 1시간 전 등)
 */
private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        seconds < 60 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        hours < 24 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> {
            val sdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
