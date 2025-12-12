package com.mop.phishing.ui.screens.main

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mop.phishing.ui.components.*
import com.mop.phishing.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    notifications: List<NotificationItem>,
    unanalyzedCount: Int,
    onNotificationClick: (NotificationItem) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = { NotificationListTopBar(onSettingsClick) }, containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        LazyColumn(modifier = modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column {
                    Text("알림 목록", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    if (unanalyzedCount > 0) { Spacer(Modifier.height(4.dp)); Text("${unanalyzedCount}개의 미분석 알림", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(16.dp))
                }
            }
            if (notifications.isEmpty()) { item { EmptyNotificationView() } }
            else { items(notifications, key = { it.id }) { NotificationCard(it) { onNotificationClick(it) } } }
        }
    }
}

@Composable
fun NotificationListTopBar(onSettingsClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(listOf(Primary, PrimaryGradientEnd))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lock, "앱 아이콘", Color.White, Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("피싱 탐지", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text("실시간 URL 보호", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "설정", MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun EmptyNotificationView(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(PrimaryLight.copy(0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Lock, null, Primary, Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("감지된 알림이 없습니다", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("새로운 알림이 오면\n자동으로 URL을 검사합니다", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
