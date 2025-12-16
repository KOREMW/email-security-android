package com.mop.phishing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mop.phishing.ui.theme.*

enum class NotificationSource {
    KAKAOTALK, SMS, GMAIL, NAVER, OTHER
}

data class NotificationItem(
    val id: String,
    val source: NotificationSource,
    val title: String,
    val content: String,
    val url: String,
    val timeAgo: String,
    val riskLevel: RiskLevel,
    val riskScore: Int = 0,
    val isAnalyzed: Boolean = false,
    // 상세 분석 결과 (DB에서 로드)
    val localMlScore: Float = 0f,
    val virusTotalMalicious: Int = 0,
    val virusTotalSuspicious: Int = 0,
    val virusTotalTotal: Int = 0,
    val finalScore: Int = 0,
    val aiExplanation: String? = null,
    val recommendation: String? = null,
    val canAccess: Boolean = true
)

@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SourceIcon(source = item.source)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = getSourceName(item.source), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                if (item.isAnalyzed) { RiskBadge(riskLevel = item.riskLevel) }
                Spacer(modifier = Modifier.weight(1f))
                Text(text = item.timeAgo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "상세보기", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(12.dp))
            UrlChip(url = item.url)
        }
    }
}

@Composable
fun SourceIcon(source: NotificationSource, modifier: Modifier = Modifier) {
    val (backgroundColor, icon) = when (source) {
        NotificationSource.KAKAOTALK -> Pair(Color(0xFFFEE500), Icons.Default.Favorite)
        NotificationSource.SMS -> Pair(Color(0xFFFF9800), Icons.Default.Phone)
        NotificationSource.GMAIL -> Pair(Color(0xFFEA4335), Icons.Default.MailOutline)
        NotificationSource.NAVER -> Pair(Color(0xFF03C75A), Icons.Default.MailOutline)
        NotificationSource.OTHER -> Pair(Primary, Icons.Default.Favorite)
    }
    Box(modifier = modifier.size(40.dp).clip(CircleShape).background(backgroundColor), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = getSourceName(source), tint = if (source == NotificationSource.KAKAOTALK) Color.Black else Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun UrlChip(url: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔗", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = url, style = MaterialTheme.typography.bodySmall, color = Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

fun getSourceName(source: NotificationSource): String = when (source) {
    NotificationSource.KAKAOTALK -> "카카오톡"
    NotificationSource.SMS -> "문자메시지"
    NotificationSource.GMAIL -> "Gmail"
    NotificationSource.NAVER -> "네이버 메일"
    NotificationSource.OTHER -> "알림"
}
