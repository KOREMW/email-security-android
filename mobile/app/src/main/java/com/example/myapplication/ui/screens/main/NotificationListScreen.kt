package com.example.myapplication.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*

/**
 * 알림 목록 화면 (메인 화면)
 * 목업 이미지 1, 2 참고
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    notifications: List<NotificationItem>,
    unanalyzedCount: Int,
    onNotificationClick: (NotificationItem) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            NotificationListTopBar(onSettingsClick = onSettingsClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 헤더
            item {
                Column {
                    Text(
                        text = "알림 목록",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    if (unanalyzedCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${unanalyzedCount}개의 미분석 알림",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 알림이 없을 때
            if (notifications.isEmpty()) {
                item {
                    EmptyNotificationView()
                }
            } else {
                // 알림 목록
                items(
                    items = notifications,
                    key = { it.id }
                ) { notification ->
                    NotificationCard(
                        item = notification,
                        onClick = { onNotificationClick(notification) }
                    )
                }
            }
        }
    }
}

/**
 * 상단 앱바
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListTopBar(
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 앱 아이콘
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Primary, PrimaryGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "앱 아이콘",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "피싱 탐지",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "실시간 URL 보호",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 알림이 없을 때 표시되는 뷰
 */
@Composable
fun EmptyNotificationView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PrimaryLight.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "감지된 알림이 없습니다",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "새로운 알림이 오면\n자동으로 URL을 검사합니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotificationListScreenPreview() {
    EmailSecurityTheme {
        NotificationListScreen(
            notifications = listOf(
                NotificationItem(
                    id = "1",
                    source = NotificationSource.KAKAOTALK,
                    title = "새 메시지",
                    content = "안녕하세요! 이벤트 참여하시면 경품을 받으실 수 있습니다...",
                    url = "https://kakao-event-2...",
                    timeAgo = "방금 전",
                    riskLevel = RiskLevel.DANGER,
                    isAnalyzed = true
                ),
                NotificationItem(
                    id = "2",
                    source = NotificationSource.SMS,
                    title = "[Web발신]",
                    content = "[국민은행] 고객님의 보안카드가 만료되었습니다. 아래 링크에...",
                    url = "http://kbbank-secure....",
                    timeAgo = "5분 전",
                    riskLevel = RiskLevel.UNKNOWN,
                    isAnalyzed = false
                ),
                NotificationItem(
                    id = "3",
                    source = NotificationSource.GMAIL,
                    title = "Your Amazon Order",
                    content = "Your package has been delivered. Track your orde...",
                    url = "https://amazon.com/tr...",
                    timeAgo = "1시간 전",
                    riskLevel = RiskLevel.SAFE,
                    isAnalyzed = true
                )
            ),
            unanalyzedCount = 2,
            onNotificationClick = {},
            onSettingsClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyNotificationListScreenPreview() {
    EmailSecurityTheme {
        NotificationListScreen(
            notifications = emptyList(),
            unanalyzedCount = 0,
            onNotificationClick = {},
            onSettingsClick = {}
        )
    }
}
