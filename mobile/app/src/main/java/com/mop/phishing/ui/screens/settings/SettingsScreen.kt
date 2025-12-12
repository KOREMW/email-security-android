package com.mop.phishing.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onBackClick: () -> Unit,
    onProfileEditClick: () -> Unit,
    onSettingChange: (SettingsState) -> Unit,
    onVersionClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onOpenSourceClick: () -> Unit,
    onResetOnboardingClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("설정", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기") } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(MaterialTheme.colorScheme.background))
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ProfileCard(settingsState.userName, settingsState.userEmail, onProfileEditClick)

            SettingsSection("보안 설정", Icons.Default.Lock) {
                SettingsToggleItem("자동 분석", "알림 수신 시 자동으로 URL 분석", settingsState.autoAnalysis) { onSettingChange(settingsState.copy(autoAnalysis = it)) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                SettingsToggleItem("고위험 자동 차단", "위험도 높은 URL 자동 차단", settingsState.autoBlockHighRisk) { onSettingChange(settingsState.copy(autoBlockHighRisk = it)) }
            }

            SettingsSection("알림 설정", Icons.Default.Star) {
                SettingsToggleItem("푸시 알림", "위험 탐지 시 알림 받기", settingsState.pushNotification) { onSettingChange(settingsState.copy(pushNotification = it)) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                SettingsToggleItem("소리 알림", "위험 탐지 시 소리로 알림", settingsState.soundNotification) { onSettingChange(settingsState.copy(soundNotification = it)) }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                SettingsToggleItem("진동", "위험 탐지 시 진동 알림", settingsState.vibration) { onSettingChange(settingsState.copy(vibration = it)) }
            }

            SettingsSection("화면 설정", Icons.Default.Settings) {
                SettingsToggleItem("다크 모드", "어두운 테마 사용", settingsState.darkMode) { onSettingChange(settingsState.copy(darkMode = it)) }
            }

            SettingsSection("정보", Icons.Default.Info) {
                SettingsNavigationItem("버전 정보", "v1.0.0", onVersionClick)
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                SettingsNavigationItem("이용 약관", onClick = onTermsClick)
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                SettingsNavigationItem("개인정보 처리방침", onClick = onPrivacyClick)
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                SettingsNavigationItem("오픈소스 라이선스", onClick = onOpenSourceClick)
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(0.2f))
                SettingsNavigationItem("온보딩 다시 보기", onClick = onResetOnboardingClick)
            }

            LogoutButton(onLogoutClick)
            Spacer(Modifier.height(16.dp))
        }
    }
}
