package com.mop.phishing.ui.screens.settings

<<<<<<< HEAD
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
=======
import androidx.compose.foundation.layout.*
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
<<<<<<< HEAD
import androidx.compose.material.icons.filled.Block
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
=======
import androidx.compose.ui.unit.dp
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD
    // 알림 리스너 서비스 상태
    isNotificationListenerEnabled: Boolean = false,
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    onBackClick: () -> Unit,
    onLoginClick: (() -> Unit)? = null,
    onLogoutClick: () -> Unit,
    onSyncClick: (() -> Unit)? = null,
    onSettingChange: (SettingsState) -> Unit,
    // URL 차단 서비스 활성화 콜백
    onAccessibilityClick: (() -> Unit)? = null,
    onOverlayPermissionClick: (() -> Unit)? = null,
<<<<<<< HEAD
    // 알림 리스너 서비스 활성화 콜백
    onNotificationListenerClick: (() -> Unit)? = null,
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    onVersionClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    onResetOnboardingClick: () -> Unit,
    onClearHistoryClick: (() -> Unit)? = null,
<<<<<<< HEAD
    // 차단된 URL 목록 (위험도 포함)
    blockedUrlsWithRisk: Map<String, RiskLevel?> = emptyMap(),
    onUnblockUrl: ((String) -> Unit)? = null,
    onTestSafeClick: (() -> Unit)? = null,
    onTestSuspiciousClick: (() -> Unit)? = null,
    onTestDangerousClick: (() -> Unit)? = null,
    onMlTestClick: (() -> Unit)? = null,  // ML 모델 배치 테스트
    modifier: Modifier = Modifier
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showBlockedUrlsDialog by remember { mutableStateOf(false) }
    
    // 차단된 URL 목록 다이얼로그
    if (showBlockedUrlsDialog) {
        BlockedUrlsDialog(
            blockedUrlsWithRisk = blockedUrlsWithRisk,
            onDismiss = { showBlockedUrlsDialog = false },
            onUnblockUrl = { url ->
                onUnblockUrl?.invoke(url)
            }
        )
    }
=======
    onTestSafeClick: (() -> Unit)? = null,
    onTestSuspiciousClick: (() -> Unit)? = null,
    onTestDangerousClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showClearHistoryDialog by remember { mutableStateOf(false) }
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
    
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
            
<<<<<<< HEAD
            // 필수 권한 설정 (3개 통합)
            SettingsSection(title = "필수 권한 설정", icon = Icons.Default.Shield) {
                // 전체 상태 표시
                val allEnabled = isNotificationListenerEnabled && isAccessibilityEnabled && isOverlayEnabled
                val enabledCount = listOf(isNotificationListenerEnabled, isAccessibilityEnabled, isOverlayEnabled).count { it }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            allEnabled -> SafeGreen.copy(alpha = 0.1f)
                            enabledCount == 0 -> DangerRed.copy(alpha = 0.1f)
                            else -> WarningYellow.copy(alpha = 0.1f)
                        }
=======
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
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
<<<<<<< HEAD
                            imageVector = if (allEnabled) Icons.Default.Security else Icons.Default.Shield,
                            contentDescription = null,
                            tint = when {
                                allEnabled -> SafeGreen
                                enabledCount == 0 -> DangerRed
                                else -> WarningYellow
                            },
=======
                            imageVector = if (isFullyEnabled) Icons.Default.Security else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isFullyEnabled) SafeGreen else WarningYellow,
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
<<<<<<< HEAD
                                text = if (allEnabled) "모든 권한 활성화됨" else "권한 설정 필요 ($enabledCount/3)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    allEnabled -> SafeGreen
                                    enabledCount == 0 -> DangerRed
                                    else -> WarningYellow
                                }
                            )
                            Text(
                                text = if (allEnabled)
                                    "알림 감지 및 URL 차단이 정상 작동합니다"
                                else
                                    "아래 권한을 모두 활성화해주세요",
=======
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
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
<<<<<<< HEAD

                Spacer(Modifier.height(8.dp))

                // 1. 알림 접근 권한
                PermissionSettingItem(
                    title = "알림 접근 권한",
                    description = "메일/문자/카카오톡 알림에서 URL 감지",
                    isEnabled = isNotificationListenerEnabled,
                    onClick = { onNotificationListenerClick?.invoke() }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // 2. 접근성 서비스
                PermissionSettingItem(
                    title = "접근성 서비스",
                    description = "브라우저에서 위험 URL 감지",
                    isEnabled = isAccessibilityEnabled,
                    onClick = { onAccessibilityClick?.invoke() }
                )

=======
                
                Spacer(Modifier.height(8.dp))
                
                // 접근성 서비스
                PermissionSettingItem(
                    title = "접근성 서비스",
                    description = "브라우저 URL 감지에 필요",
                    isEnabled = isAccessibilityEnabled,
                    onClick = { onAccessibilityClick?.invoke() }
                )
                
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
<<<<<<< HEAD

                // 3. 오버레이 권한
                PermissionSettingItem(
                    title = "다른 앱 위에 표시",
                    description = "위험 URL 차단 경고 화면 표시",
=======
                
                // 오버레이 권한
                PermissionSettingItem(
                    title = "다른 앱 위에 표시",
                    description = "차단 경고 화면 표시에 필요",
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD
                    title = "차단된 URL 목록",
                    description = "차단한 URL ${blockedUrlsWithRisk.size}개",
                    onClick = { showBlockedUrlsDialog = true }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                SettingsNavigationItem(
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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

<<<<<<< HEAD
=======
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

>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
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
<<<<<<< HEAD

/**
 * 차단된 URL 목록 다이얼로그
 */
@Composable
fun BlockedUrlsDialog(
    blockedUrlsWithRisk: Map<String, RiskLevel?>,
    onDismiss: () -> Unit,
    onUnblockUrl: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 헤더
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "차단된 URL 목록",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${blockedUrlsWithRisk.size}개",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                if (blockedUrlsWithRisk.isEmpty()) {
                    // 빈 상태
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "차단된 URL이 없습니다",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // URL 목록
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(blockedUrlsWithRisk.entries.toList()) { (url, riskLevel) ->
                            BlockedUrlItem(
                                url = url,
                                riskLevel = riskLevel,
                                onUnblock = { onUnblockUrl(url) }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 닫기 버튼
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary
                    )
                ) {
                    Text("닫기")
                }
            }
        }
    }
}

/**
 * 차단된 URL 아이템 (위험도 표시)
 */
@Composable
fun BlockedUrlItem(
    url: String,
    riskLevel: RiskLevel?,
    onUnblock: () -> Unit
) {
    // 위험도별 색상 및 텍스트
    val (bgColor, riskText, riskColor) = when (riskLevel) {
        RiskLevel.SAFE -> Triple(SafeGreen.copy(alpha = 0.1f), "안전", SafeGreen)
        RiskLevel.CAUTION -> Triple(WarningYellow.copy(alpha = 0.1f), "주의", WarningYellow)
        RiskLevel.DANGER -> Triple(DangerRed.copy(alpha = 0.1f), "위험", DangerRed)
        RiskLevel.UNKNOWN, null -> Triple(MaterialTheme.colorScheme.surfaceVariant, "미분석", MaterialTheme.colorScheme.onSurfaceVariant)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = bgColor
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 위험도 배지
                Surface(
                    color = riskColor.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = riskText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = riskColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(Modifier.width(8.dp))
            
            // 차단 해제 버튼
            OutlinedButton(
                onClick = onUnblock,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = riskColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "해제",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
=======
>>>>>>> 0b337cd2293a0ad0b734fa9b838002e71ecac5e8
