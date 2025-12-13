package com.mop.phishing.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mop.phishing.ui.components.*
import com.mop.phishing.ui.theme.*

data class SettingsState(
    val userName: String = "사용자",
    val userEmail: String = "user@example.com",
    val autoAnalysis: Boolean = true,
    val autoBlockHighRisk: Boolean = false,
    val pushNotification: Boolean = true,
    val soundNotification: Boolean = true,
    val vibration: Boolean = true,
    val darkMode: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    isLoggedIn: Boolean = false,
    isSyncing: Boolean = false,
    lastSyncMessage: String? = null,
    // URL 차단 서비스 상태
    isAccessibilityEnabled: Boolean = false,
    isOverlayEnabled: Boolean = false,
    onBackClick: () -> Unit,
    onLoginClick: (() -> Unit)? = null,
    onLogoutClick: () -> Unit,
    onSyncClick: (() -> Unit)? = null,
    onSettingChange: (SettingsState) -> Unit,
    // URL 차단 서비스 활성화 콜백
    onAccessibilityClick: (() -> Unit)? = null,
    onOverlayPermissionClick: (() -> Unit)? = null,
    onVersionClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    onResetOnboardingClick: () -> Unit,
    onClearHistoryClick: (() -> Unit)? = null,
    onTestSafeClick: (() -> Unit)? = null,
    onTestSuspiciousClick: (() -> Unit)? = null,
    onTestDangerousClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    
    // 분석 기록 삭제 확인 다이얼로그
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("분석 기록 삭제") },
            text = { Text("모든 분석 기록을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        onClearHistoryClick?.invoke()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("설정", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileCard(
                userName = settingsState.userName,
                userEmail = settingsState.userEmail,
                isLoggedIn = isLoggedIn,
                onLogoutClick = onLogoutClick,
                onLoginClick = onLoginClick
            )

            // 클라우드 동기화 섹션 (로그인 시에만 표시)
            if (isLoggedIn) {
                SettingsSection(title = "클라우드 동기화", icon = Icons.Default.Cloud) {
                    SettingsNavigationItem(
                        title = if (isSyncing) "동기화 중..." else "지금 동기화",
                        description = lastSyncMessage ?: "분석 기록과 차단 URL을 클라우드에 저장합니다",
                        onClick = { if (!isSyncing) onSyncClick?.invoke() }
                    )
                }
            }
            
            // 🆕 URL 차단 서비스 섹션
            SettingsSection(title = "URL 차단 서비스", icon = Icons.Default.Shield) {
                // 전체 상태 표시
                val isFullyEnabled = isAccessibilityEnabled && isOverlayEnabled
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFullyEnabled) 
                            SafeGreen.copy(alpha = 0.1f) 
                        else 
                            WarningYellow.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFullyEnabled) Icons.Default.Security else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isFullyEnabled) SafeGreen else WarningYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isFullyEnabled) "차단 서비스 활성화됨" else "차단 서비스 비활성화",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isFullyEnabled) SafeGreen else WarningYellow
                            )
                            Text(
                                text = if (isFullyEnabled) 
                                    "브라우저에서 위험 URL 접근 시 자동 차단됩니다" 
                                else 
                                    "아래 권한을 활성화해주세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // 접근성 서비스
                PermissionSettingItem(
                    title = "접근성 서비스",
                    description = "브라우저 URL 감지에 필요",
                    isEnabled = isAccessibilityEnabled,
                    onClick = { onAccessibilityClick?.invoke() }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                // 오버레이 권한
                PermissionSettingItem(
                    title = "다른 앱 위에 표시",
                    description = "차단 경고 화면 표시에 필요",
                    isEnabled = isOverlayEnabled,
                    onClick = { onOverlayPermissionClick?.invoke() }
                )
            }

            SettingsSection(title = "보안 설정", icon = Icons.Default.Lock) {
                SettingsToggleItem(
                    title = "자동 분석",
                    description = "알림 수신 시 자동으로 URL 분석",
                    checked = settingsState.autoAnalysis,
                    onCheckedChange = { onSettingChange(settingsState.copy(autoAnalysis = it)) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsToggleItem(
                    title = "고위험 자동 차단",
                    description = "위험도 높은 URL 자동 차단",
                    checked = settingsState.autoBlockHighRisk,
                    onCheckedChange = { onSettingChange(settingsState.copy(autoBlockHighRisk = it)) }
                )
            }

            SettingsSection(title = "알림 설정", icon = Icons.Default.Star) {
                SettingsToggleItem(
                    title = "푸시 알림",
                    description = "위험 탐지 시 알림 받기",
                    checked = settingsState.pushNotification,
                    onCheckedChange = { onSettingChange(settingsState.copy(pushNotification = it)) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsToggleItem(
                    title = "소리 알림",
                    description = "위험 탐지 시 소리로 알림",
                    checked = settingsState.soundNotification,
                    onCheckedChange = { onSettingChange(settingsState.copy(soundNotification = it)) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsToggleItem(
                    title = "진동",
                    description = "위험 탐지 시 진동 알림",
                    checked = settingsState.vibration,
                    onCheckedChange = { onSettingChange(settingsState.copy(vibration = it)) }
                )
            }

            SettingsSection(title = "화면 설정", icon = Icons.Default.Settings) {
                SettingsToggleItem(
                    title = "다크 모드",
                    description = "어두운 테마 사용",
                    checked = settingsState.darkMode,
                    onCheckedChange = { onSettingChange(settingsState.copy(darkMode = it)) }
                )
            }
            
            // 데이터 관리 섹션 추가
            SettingsSection(title = "데이터 관리", icon = Icons.Default.Delete) {
                SettingsNavigationItem(
                    title = "분석 기록 삭제",
                    description = "모든 알림 및 분석 기록 삭제",
                    onClick = { showClearHistoryDialog = true }
                )
            }

            SettingsSection(title = "정보", icon = Icons.Default.Info) {
                SettingsNavigationItem(
                    title = "버전 정보",
                    value = "v1.0.0",
                    onClick = onVersionClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsNavigationItem(
                    title = "이용 약관",
                    onClick = onTermsClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsNavigationItem(
                    title = "개인정보 처리방침",
                    onClick = onPrivacyClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsNavigationItem(
                    title = "오픈소스 라이선스",
                    onClick = onOpenSourceClick
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsNavigationItem(
                    title = "온보딩 다시 보기",
                    onClick = onResetOnboardingClick
                )
            }

            // 개발자 옵션 (테스트)
            SettingsSection(title = "개발자 옵션", icon = Icons.Default.Build) {
                SettingsNavigationItem(
                    title = "안전한 URL 테스트",
                    description = "정상 URL 분석 테스트",
                    onClick = { onTestSafeClick?.invoke() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsNavigationItem(
                    title = "의심 URL 테스트",
                    description = "피싱 의심 URL 분석 테스트",
                    onClick = { onTestSuspiciousClick?.invoke() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsNavigationItem(
                    title = "위험 URL 테스트",
                    description = "악성 URL 분석 테스트",
                    onClick = { onTestDangerousClick?.invoke() }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 권한 설정 아이템
 */
@Composable
fun PermissionSettingItem(
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isEnabled) {
            // 활성화됨 배지
            Surface(
                color = SafeGreen.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "✓ 활성화됨",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = SafeGreen
                )
            }
        } else {
            // 활성화 버튼
            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                )
            ) {
                Text("활성화")
            }
        }
    }
}
