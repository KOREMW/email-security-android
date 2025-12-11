package com.example.myapplication.ui.components

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
import com.example.myapplication.ui.theme.*

/**
 * 알림 소스 타입
 */
enum class NotificationSource {
    KAKAOTALK,
    SMS,
    GMAIL,
    OTHER
}

/**
 * 알림 데이터 클래스
 */
data class NotificationItem(
    val id: String,
    val source: NotificationSource,
    val title: String,
    val content: String,
    val url: String,
    val timeAgo: String,
    val riskLevel: RiskLevel,
    val riskScore: Int = 0,
    val isAnalyzed: Boolean = false
)

/**
 * 알림 카드 컴포넌트
 * 목업의 알림 목록 카드 디자인 구현
 */
@Composable
fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 상단: 아이콘 + 앱 이름 + 배지 + 시간 + 화살표
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 앱 아이콘
                SourceIcon(source = item.source)

                Spacer(modifier = Modifier.width(12.dp))

                // 앱 이름
                Text(
                    text = getSourceName(item.source),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 위험도 배지 (분석 완료된 경우만)
                if (item.isAnalyzed) {
                    RiskBadge(riskLevel = item.riskLevel)
                }

                Spacer(modifier = Modifier.weight(1f))

                // 시간
                Text(
                    text = item.timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "상세보기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 제목
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 내용 미리보기
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // URL 칩
            UrlChip(url = item.url)
        }
    }
}

/**
 * 소스별 아이콘
 */
@Composable
fun SourceIcon(
    source: NotificationSource,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, icon) = when (source) {
        NotificationSource.KAKAOTALK -> Pair(Color(0xFFFEE500), Icons.Default.Favorite)
        NotificationSource.SMS -> Pair(Color(0xFFFF9800), Icons.Default.Phone)
        NotificationSource.GMAIL -> Pair(Color(0xFFEA4335), Icons.Default.MailOutline)
        NotificationSource.OTHER -> Pair(Primary, Icons.Default.Favorite)
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = getSourceName(source),
            tint = if (source == NotificationSource.KAKAOTALK) Color.Black else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * URL 칩 컴포넌트
 */
@Composable
fun UrlChip(
    url: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔗",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 소스 이름 반환
 */
fun getSourceName(source: NotificationSource): String {
    return when (source) {
        NotificationSource.KAKAOTALK -> "카카오톡"
        NotificationSource.SMS -> "문자메시지"
        NotificationSource.GMAIL -> "Gmail"
        NotificationSource.OTHER -> "알림"
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationCardPreview() {
    EmailSecurityTheme {
        NotificationCard(
            item = NotificationItem(
                id = "1",
                source = NotificationSource.KAKAOTALK,
                title = "새 메시지",
                content = "안녕하세요! 이벤트 참여하시면 경품을 받으실 수 있습니다...",
                url = "https://kakao-event-2...",
                timeAgo = "방금 전",
                riskLevel = RiskLevel.DANGER,
                isAnalyzed = true
            ),
            onClick = {}
        )
    }
}
