package com.mop.phishing.ui.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mop.phishing.NotificationViewModel
import com.mop.phishing.SettingsViewModel
import com.mop.phishing.UrlAnalysisViewModel
import com.mop.phishing.auth.AuthViewModel
import com.mop.phishing.auth.AuthState
import com.mop.phishing.auth.SyncState
import com.mop.phishing.data.local.SettingsData
import com.mop.phishing.ui.components.*
import com.mop.phishing.ui.screens.detail.*
import com.mop.phishing.ui.screens.main.*
import com.mop.phishing.ui.screens.onboarding.*
import com.mop.phishing.ui.screens.settings.*
import com.mop.phishing.ui.theme.DangerRed
import com.mop.phishing.ui.theme.Primary
import com.mop.phishing.ui.theme.WarningYellow
import com.mop.phishing.util.AccessibilityUtils
import com.mop.phishing.util.TestNotificationHelper
import com.mop.phishing.util.UrlActionUtils
import com.mop.phishing.util.UrlBlockManager
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object NotificationList : Screen("notification_list")
    object AnalysisDetail : Screen("analysis_detail/{notificationId}") {
        fun createRoute(notificationId: String) = "analysis_detail/$notificationId"
    }
    object Settings : Screen("settings")
    object UrlInput : Screen("url_input")
    object QrScanner : Screen("qr_scanner")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Onboarding.route,
    settingsViewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onGoogleSignInClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // ViewModels
    val notificationViewModel: NotificationViewModel = viewModel()
    val analysisViewModel: UrlAnalysisViewModel = viewModel()

    // 알림 목록 (Repository에서 실시간 업데이트)
    val notifications by notificationViewModel.notifications.collectAsState()
    val unanalyzedCount by notificationViewModel.unanalyzedCount.collectAsState()

    // 인증 상태
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val syncState by authViewModel.syncState.collectAsState()

    // 설정 상태 (DataStore에서 실시간 업데이트)
    val settingsData by settingsViewModel.settings.collectAsState()

    // SettingsData를 SettingsState로 변환 (UI 호환성)
    val settingsState = SettingsState(
        userName = if (isLoggedIn) currentUser?.displayName ?: "사용자" else settingsData.userName,
        userEmail = if (isLoggedIn) currentUser?.email ?: "" else settingsData.userEmail,
        autoAnalysis = settingsData.autoAnalysis,
        autoBlockHighRisk = settingsData.autoBlockHighRisk,
        pushNotification = settingsData.pushNotification,
        soundNotification = settingsData.soundNotification,
        vibration = settingsData.vibration,
        darkMode = settingsData.darkMode
    )

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Screen.NotificationList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.NotificationList.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                onLoginClick = onGoogleSignInClick
            )
        }

        composable(Screen.NotificationList.route) {
            val lifecycleOwner = LocalLifecycleOwner.current
            
            // 권한 상태 확인
            var isAccessibilityEnabled by remember { 
                mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) 
            }
            var isOverlayEnabled by remember { 
                mutableStateOf(AccessibilityUtils.canDrawOverlays(context)) 
            }
            
            // 화면 돌아올 때 권한 상태 갱신
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                        isOverlayEnabled = AccessibilityUtils.canDrawOverlays(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            NotificationListScreenWithFab(
                notifications = notifications,
                unanalyzedCount = unanalyzedCount,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isOverlayEnabled = isOverlayEnabled,
                onNotificationClick = { notification ->
                    analysisViewModel.resetAnalysis()
                    navController.navigate(Screen.AnalysisDetail.createRoute(notification.id))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onUrlInputClick = {
                    analysisViewModel.resetAnalysis()
                    navController.navigate(Screen.UrlInput.route)
                },
                onQrScanClick = { navController.navigate(Screen.QrScanner.route) },
                onAccessibilityClick = {
                    Toast.makeText(context, "접근성 설정에서 '피싱 URL 차단 서비스'를 활성화해주세요", Toast.LENGTH_LONG).show()
                    AccessibilityUtils.openAccessibilitySettings(context)
                },
                onOverlayPermissionClick = {
                    Toast.makeText(context, "다른 앱 위에 표시 권한을 활성화해주세요", Toast.LENGTH_LONG).show()
                    AccessibilityUtils.openOverlayPermissionSettings(context)
                }
            )
        }

        composable(
            route = Screen.AnalysisDetail.route,
            arguments = listOf(navArgument("notificationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
            val notification = notifications.find { it.id == notificationId }

            if (notification != null) {
                val analysisState by analysisViewModel.analysisState.collectAsState()
                val isLoading by analysisViewModel.isLoading.collectAsState()
                val isFromCache by analysisViewModel.isFromCache.collectAsState()
                var analysisSteps by remember { mutableStateOf(createInitialAnalysisSteps()) }
                
                var isBlocked by remember { mutableStateOf(UrlBlockManager.isBlocked(context, notification.url)) }
                var showUnblockWarningDialog by remember { mutableStateOf(false) }

                LaunchedEffect(notification.id) {
                    analysisViewModel.analyzeNotification(
                        notificationId = notification.id,
                        url = notification.url,
                        source = "notification"
                    )
                }

                LaunchedEffect(isFromCache) {
                    if (isFromCache) {
                        analysisSteps = createCompletedAnalysisSteps()
                    }
                }

                LaunchedEffect(isLoading, analysisState) {
                    if (isLoading && !isFromCache) {
                        val steps = createInitialAnalysisSteps().toMutableList()
                        steps[0] = steps[0].copy(status = StepStatus.COMPLETED)
                        steps[1] = steps[1].copy(status = StepStatus.LOADING)
                        analysisSteps = steps.toList()
                        
                        kotlinx.coroutines.delay(300)
                        steps[1] = steps[1].copy(status = StepStatus.COMPLETED)
                        steps[2] = steps[2].copy(status = StepStatus.LOADING)
                        steps[3] = steps[3].copy(status = StepStatus.LOADING)
                        analysisSteps = steps.toList()
                    } else if (!isLoading && analysisState is AnalysisState.Completed) {
                        analysisSteps = createCompletedAnalysisSteps()
                    }
                }
                
                LaunchedEffect(analysisState) {
                    if (analysisState is AnalysisState.Completed) {
                        val result = (analysisState as AnalysisState.Completed).result
                        if (result.riskLevel == RiskLevel.DANGER && !isBlocked) {
                            UrlBlockManager.blockUrl(context, notification.url)
                            isBlocked = true
                            Toast.makeText(context, "⚠️ 위험한 URL이 자동 차단되었습니다", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                val displayState = analysisState
                
                if (showUnblockWarningDialog) {
                    AlertDialog(
                        onDismissRequest = { showUnblockWarningDialog = false },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(48.dp)
                            )
                        },
                        title = {
                            Text(
                                text = "⚠️ 위험한 URL입니다",
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        },
                        text = {
                            Column {
                                Text("이 URL은 피싱 또는 악성 사이트로 탐지되었습니다.", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(12.dp))
                                Text("차단을 해제하면 다음과 같은 위험이 있습니다:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Text("• 개인정보 유출 위험\n• 금융 피해 위험\n• 악성코드 감염 위험", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(12.dp))
                                Text("정말 차단을 해제하시겠습니까?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    UrlBlockManager.unblockUrl(context, notification.url)
                                    isBlocked = false
                                    showUnblockWarningDialog = false
                                    Toast.makeText(context, "차단이 해제되었습니다. 주의하세요!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                            ) { Text("위험 감수하고 해제") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showUnblockWarningDialog = false }) { Text("취소") }
                        }
                    )
                }

                AnalysisDetailScreen(
                    notification = notification,
                    analysisState = displayState,
                    analysisSteps = analysisSteps,
                    onBackClick = { navController.popBackStack() },
                    onShareClick = {
                        if (displayState is AnalysisState.Completed) {
                            UrlActionUtils.shareAnalysisResult(context, notification.url, displayState.result)
                        } else {
                            UrlActionUtils.shareUrl(context, notification.url)
                        }
                    },
                    onBlockClick = {
                        if (isBlocked) {
                            val currentRiskLevel = (displayState as? AnalysisState.Completed)?.result?.riskLevel
                            if (currentRiskLevel == RiskLevel.DANGER) {
                                showUnblockWarningDialog = true
                            } else {
                                UrlBlockManager.unblockUrl(context, notification.url)
                                isBlocked = false
                                Toast.makeText(context, "차단이 해제되었습니다", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            UrlBlockManager.blockUrl(context, notification.url)
                            isBlocked = true
                            Toast.makeText(context, "URL이 차단되었습니다", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isBlocked = isBlocked
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, null, Modifier.size(64.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("알림을 찾을 수 없습니다", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) { Text("돌아가기") }
                    }
                }
            }
        }

        composable(Screen.UrlInput.route) {
            val analysisState by analysisViewModel.analysisState.collectAsState()
            val isLoading by analysisViewModel.isLoading.collectAsState()
            UrlInputScreen(
                analysisState = if (analysisState is AnalysisState.Loading && !isLoading) null else analysisState,
                isLoading = isLoading,
                onBackClick = { analysisViewModel.resetAnalysis(); navController.popBackStack() },
                onAnalyzeClick = { url -> analysisViewModel.analyzeUrl(url, "manual", true) },
                onQrScanClick = { navController.navigate(Screen.QrScanner.route) }
            )
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onBackClick = { navController.popBackStack() },
                onUrlScanned = { url ->
                    analysisViewModel.resetAnalysis()
                    navController.popBackStack()
                    navController.navigate(Screen.UrlInput.route)
                    analysisViewModel.analyzeUrl(url, "qr", true)
                }
            )
        }

        composable(Screen.Settings.route) {
            val testScope = rememberCoroutineScope()
            val lifecycleOwner = LocalLifecycleOwner.current

            var isAccessibilityEnabled by remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) }
            var isOverlayEnabled by remember { mutableStateOf(AccessibilityUtils.canDrawOverlays(context)) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                        isOverlayEnabled = AccessibilityUtils.canDrawOverlays(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val isSyncing = syncState is SyncState.Syncing
            val lastSyncMessage = when (syncState) {
                is SyncState.Success -> "동기화 완료: 업로드 ${(syncState as SyncState.Success).result.uploadedCount}개, 다운로드 ${(syncState as SyncState.Success).result.downloadedCount}개"
                is SyncState.Error -> "동기화 실패: ${(syncState as SyncState.Error).message}"
                is SyncState.Syncing -> "동기화 중..."
                else -> null
            }

            SettingsScreen(
                settingsState = settingsState,
                isLoggedIn = isLoggedIn,
                isSyncing = isSyncing,
                lastSyncMessage = lastSyncMessage,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isOverlayEnabled = isOverlayEnabled,
                onBackClick = { navController.popBackStack() },
                onLoginClick = onGoogleSignInClick,
                onLogoutClick = { if (isLoggedIn) { authViewModel.signOut(); Toast.makeText(context, "로그아웃되었습니다", Toast.LENGTH_SHORT).show() } },
                onSyncClick = { authViewModel.syncData(); Toast.makeText(context, "동기화 시작...", Toast.LENGTH_SHORT).show() },
                onAccessibilityClick = { Toast.makeText(context, "접근성 설정에서 '피싱 URL 차단 서비스'를 활성화해주세요", Toast.LENGTH_LONG).show(); AccessibilityUtils.openAccessibilitySettings(context) },
                onOverlayPermissionClick = { Toast.makeText(context, "다른 앱 위에 표시 권한을 활성화해주세요", Toast.LENGTH_LONG).show(); AccessibilityUtils.openOverlayPermissionSettings(context) },
                onSettingChange = { newState ->
                    if (newState.darkMode != settingsState.darkMode) settingsViewModel.updateDarkMode(newState.darkMode)
                    if (newState.autoAnalysis != settingsState.autoAnalysis) settingsViewModel.updateAutoAnalysis(newState.autoAnalysis)
                    if (newState.autoBlockHighRisk != settingsState.autoBlockHighRisk) settingsViewModel.updateAutoBlockHighRisk(newState.autoBlockHighRisk)
                    if (newState.pushNotification != settingsState.pushNotification) settingsViewModel.updatePushNotification(newState.pushNotification)
                    if (newState.soundNotification != settingsState.soundNotification) settingsViewModel.updateSoundNotification(newState.soundNotification)
                    if (newState.vibration != settingsState.vibration) settingsViewModel.updateVibration(newState.vibration)
                },
                onVersionClick = { Toast.makeText(context, "Email Security v1.0.0\nPowered by ML & Cloud Functions", Toast.LENGTH_LONG).show() },
                onTermsClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/terms"))) } catch (e: Exception) { Toast.makeText(context, "이용 약관을 열 수 없습니다", Toast.LENGTH_SHORT).show() } },
                onPrivacyClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com/privacy"))) } catch (e: Exception) { Toast.makeText(context, "개인정보 처리방침을 열 수 없습니다", Toast.LENGTH_SHORT).show() } },
                onOpenSourceClick = { Toast.makeText(context, "사용된 오픈소스:\n• TensorFlow Lite\n• Retrofit\n• Jetpack Compose\n• Room Database", Toast.LENGTH_LONG).show() },
                onResetOnboardingClick = { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("onboarding_completed", false).apply(); navController.navigate(Screen.Onboarding.route) { popUpTo(0) { inclusive = true } } },
                onClearHistoryClick = { notificationViewModel.deleteAllNotifications(); Toast.makeText(context, "분석 기록이 삭제되었습니다", Toast.LENGTH_SHORT).show() },
                onTestSafeClick = { testScope.launch { Toast.makeText(context, "안전한 URL 테스트 중...", Toast.LENGTH_SHORT).show(); val result = TestNotificationHelper.quickTest(context, TestNotificationHelper.TestScenario.SAFE_EMAIL); Toast.makeText(context, "${if (result.isCached) "[캐시]" else "[새분석]"} 점수: ${result.riskScore}% (${result.riskLevel})", Toast.LENGTH_LONG).show() } },
                onTestSuspiciousClick = { testScope.launch { Toast.makeText(context, "의심 URL 테스트 중...", Toast.LENGTH_SHORT).show(); val result = TestNotificationHelper.quickTest(context, TestNotificationHelper.TestScenario.SUSPICIOUS_SMS); Toast.makeText(context, "${if (result.isCached) "[캐시]" else "[새분석]"} 점수: ${result.riskScore}% (${result.riskLevel})\nVT: 악성${result.vtMalicious}/의심${result.vtSuspicious}", Toast.LENGTH_LONG).show() } },
                onTestDangerousClick = { testScope.launch { Toast.makeText(context, "위험 URL 테스트 중...", Toast.LENGTH_SHORT).show(); val result = TestNotificationHelper.quickTest(context, TestNotificationHelper.TestScenario.DANGEROUS_KAKAO); Toast.makeText(context, "${if (result.isCached) "[캐시]" else "[새분석]"} 점수: ${result.riskScore}% (${result.riskLevel})\nVT: 악성${result.vtMalicious}/의심${result.vtSuspicious}", Toast.LENGTH_LONG).show() } }
            )
        }
    }
}

/**
 * 권한 안내 배너가 포함된 메인 화면
 */
@Composable
fun NotificationListScreenWithFab(
    notifications: List<NotificationItem>,
    unanalyzedCount: Int,
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    onNotificationClick: (NotificationItem) -> Unit,
    onSettingsClick: () -> Unit,
    onUrlInputClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onOverlayPermissionClick: () -> Unit
) {
    var showFabMenu by remember { mutableStateOf(false) }
    // 배너 닫기 상태 (세션 동안만 유지)
    var isBannerDismissed by remember { mutableStateOf(false) }
    
    val needsPermissions = !isAccessibilityEnabled || !isOverlayEnabled
    
    Scaffold(
        floatingActionButton = {
            ExpandableFab(
                expanded = showFabMenu,
                onExpandedChange = { showFabMenu = it },
                onUrlInputClick = { showFabMenu = false; onUrlInputClick() },
                onQrScanClick = { showFabMenu = false; onQrScanClick() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { NotificationListTopBar(onSettingsClick) }
            
            // 🆕 권한 안내 배너
            if (needsPermissions && !isBannerDismissed) {
                item {
                    PermissionSetupBanner(
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isOverlayEnabled = isOverlayEnabled,
                        onAccessibilityClick = onAccessibilityClick,
                        onOverlayClick = onOverlayPermissionClick,
                        onDismiss = { isBannerDismissed = true }
                    )
                }
            }
            
            item {
                Column {
                    Text(
                        text = "알림 목록",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (unanalyzedCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text("${unanalyzedCount}개의 미분석 알림", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (notifications.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("총 ${notifications.size}개의 알림", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            
            if (notifications.isEmpty()) {
                item { EmptyNotificationView() }
            } else {
                items(notifications.size, { notifications[it].id }) { index ->
                    NotificationCard(item = notifications[index], onClick = { onNotificationClick(notifications[index]) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

/**
 * 🆕 권한 설정 안내 배너
 */
@Composable
fun PermissionSetupBanner(
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    onAccessibilityClick: () -> Unit,
    onOverlayClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WarningYellow.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = WarningYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "🛡️ URL 차단 기능 설정",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "닫기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "위험한 URL을 브라우저에서 자동으로 차단하려면 아래 권한을 활성화해주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(12.dp))
            
            // 접근성 서비스 버튼
            PermissionButton(
                title = "1. 접근성 서비스",
                description = "브라우저 URL 감지",
                isEnabled = isAccessibilityEnabled,
                onClick = onAccessibilityClick
            )
            
            Spacer(Modifier.height(8.dp))
            
            // 오버레이 권한 버튼
            PermissionButton(
                title = "2. 다른 앱 위에 표시",
                description = "차단 경고 화면 표시",
                isEnabled = isOverlayEnabled,
                onClick = onOverlayClick
            )
            
            // 모두 활성화되면 완료 메시지
            if (isAccessibilityEnabled && isOverlayEnabled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Primary.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("모든 권한이 활성화되었습니다!", style = MaterialTheme.typography.bodyMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * 권한 설정 버튼
 */
@Composable
fun PermissionButton(
    title: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isEnabled) Primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !isEnabled, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        if (isEnabled) {
            Icon(Icons.Default.CheckCircle, "활성화됨", tint = Primary, modifier = Modifier.size(24.dp))
        } else {
            Surface(color = Primary, shape = RoundedCornerShape(16.dp)) {
                Text("설정", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
    }
}

@Composable
fun ExpandableFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onUrlInputClick: () -> Unit,
    onQrScanClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimatedVisibility(visible = expanded, enter = fadeIn() + slideInVertically { it }, exit = fadeOut() + slideOutVertically { it }) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(onClick = onQrScanClick, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(8.dp)); Text("QR 스캔")
                }
                ExtendedFloatingActionButton(onClick = onUrlInputClick, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(Icons.Default.Link, null); Spacer(Modifier.width(8.dp)); Text("URL 입력")
                }
            }
        }
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }, containerColor = Primary) {
            Icon(if (expanded) Icons.Default.Close else Icons.Default.Add, if (expanded) "닫기" else "URL 검사", tint = Color.White)
        }
    }
}

private fun createInitialAnalysisSteps() = listOf(
    AnalysisStepState(AnalysisStep.URL_EXTRACT, StepStatus.PENDING),
    AnalysisStepState(AnalysisStep.LOCAL_ML, StepStatus.PENDING),
    AnalysisStepState(AnalysisStep.CLOUD_FUNCTION, StepStatus.PENDING),
    AnalysisStepState(AnalysisStep.VIRUS_TOTAL, StepStatus.PENDING),
    AnalysisStepState(AnalysisStep.OPENAI, StepStatus.PENDING)
)

private fun createCompletedAnalysisSteps() = listOf(
    AnalysisStepState(AnalysisStep.URL_EXTRACT, StepStatus.COMPLETED),
    AnalysisStepState(AnalysisStep.LOCAL_ML, StepStatus.COMPLETED),
    AnalysisStepState(AnalysisStep.CLOUD_FUNCTION, StepStatus.COMPLETED),
    AnalysisStepState(AnalysisStep.VIRUS_TOTAL, StepStatus.COMPLETED),
    AnalysisStepState(AnalysisStep.OPENAI, StepStatus.COMPLETED)
)
