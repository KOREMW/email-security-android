package com.example.myapplication.ui.screens.settings

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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*

/**
 * 설정 데이터 클래스
 */
data class SettingsState(
    val userName: String = "사용자",
    val userEmail: String = "user@example.com",
    // 보안 설정
    val autoAnalysis: Boolean = true,
    val autoBlockHighRisk: Boolean = false,
    // 알림 설정
    val pushNotification: Boolean = true,
    val soundNotification: Boolean = true,
    val vibration: Boolean = true,
    // 화면 설정
    val darkMode: Boolean = false
)

/**
 * 설정 화면
 */
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "설정",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
            // 프로필 카드
            ProfileCard(
                userName = settingsState.userName,
                userEmail = settingsState.userEmail,
                onEditClick = onProfileEditClick
            )

            // 보안 설정
            SettingsSection(
                title = "보안 설정",
                icon = Icons.Default.Lock
            ) {
                SettingsToggleItem(
                    title = "자동 분석",
                    description = "알림 수신 시 자동으로 URL 분석",
                    checked = settingsState.autoAnalysis,
                    onCheckedChange = { 
                        onSettingChange(settingsState.copy(autoAnalysis = it))
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                SettingsToggleItem(
                    title = "고위험 자동 차단",
                    description = "위험도 높은 URL 자동 차단",
                    checked = settingsState.autoBlockHighRisk,
                    onCheckedChange = {
                        onSettingChange(settingsState.copy(autoBlockHighRisk = it))
                    }
                )
            }

            // 알림 설정
            SettingsSection(
                title = "알림 설정",
                icon = Icons.Default.Star
            ) {
                SettingsToggleItem(
                    title = "푸시 알림",
                    description = "위험 탐지 시 알림 받기",
                    checked = settingsState.pushNotification,
                    onCheckedChange = {
                        onSettingChange(settingsState.copy(pushNotification = it))
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                SettingsToggleItem(
                    title = "소리 알림",
                    description = "위험 탐지 시 소리로 알림",
                    checked = settingsState.soundNotification,
                    onCheckedChange = {
                        onSettingChange(settingsState.copy(soundNotification = it))
                    }
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                
                SettingsToggleItem(
                    title = "진동",
                    description = "위험 탐지 시 진동 알림",
                    checked = settingsState.vibration,
                    onCheckedChange = {
                        onSettingChange(settingsState.copy(vibration = it))
                    }
                )
            }

            // 화면 설정
            SettingsSection(
                title = "화면 설정",
                icon = Icons.Default.Settings
            ) {
                SettingsToggleItem(
                    title = "다크 모드",
                    description = "어두운 테마 사용",
                    checked = settingsState.darkMode,
                    onCheckedChange = {
                        onSettingChange(settingsState.copy(darkMode = it))
                    }
                )
            }

            // 정보
            SettingsSection(
                title = "정보",
                icon = Icons.Default.Info
            ) {
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

            // 로그아웃 버튼
            LogoutButton(onClick = onLogoutClick)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    EmailSecurityTheme {
        SettingsScreen(
            settingsState = SettingsState(),
            onBackClick = {},
            onProfileEditClick = {},
            onSettingChange = {},
            onVersionClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onOpenSourceClick = {},
            onResetOnboardingClick = {},
            onLogoutClick = {}
        )
    }
}
