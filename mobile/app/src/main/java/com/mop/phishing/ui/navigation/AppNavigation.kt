package com.mop.phishing.ui.navigation

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mop.phishing.UrlAnalysisViewModel
import com.mop.phishing.ui.components.*
import com.mop.phishing.ui.screens.detail.*
import com.mop.phishing.ui.screens.main.*
import com.mop.phishing.ui.screens.onboarding.*
import com.mop.phishing.ui.screens.settings.*
import com.mop.phishing.ui.theme.Primary
import com.mop.phishing.util.UrlActionUtils
import com.mop.phishing.util.UrlBlockManager

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
    startDestination: String = Screen.Onboarding.route
) {
    val context = LocalContext.current
    val analysisViewModel: UrlAnalysisViewModel = viewModel()
    var notifications by remember { mutableStateOf(sampleNotifications) }
    var settingsState by remember { mutableStateOf(SettingsState()) }

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
                }
            )
        }

        composable(Screen.NotificationList.route) {
            NotificationListScreenWithFab(
                notifications = notifications,
                unanalyzedCount = notifications.count { !it.isAnalyzed },
                onNotificationClick = { notification ->
                    analysisViewModel.resetAnalysis()
                    navController.navigate(Screen.AnalysisDetail.createRoute(notification.id))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onUrlInputClick = {
                    analysisViewModel.resetAnalysis()
                    navController.navigate(Screen.UrlInput.route)
                },
                onQrScanClick = { navController.navigate(Screen.QrScanner.route) }
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
                var analysisSteps by remember { mutableStateOf(createInitialAnalysisSteps()) }

                LaunchedEffect(notification.id) {
                    if (!notification.isAnalyzed) {
                        updateStepsAndAnalyze(analysisViewModel, notification.url) { analysisSteps = it }
                    }
                }

                LaunchedEffect(analysisState) {
                    if (analysisState is AnalysisState.Completed) {
                        val result = (analysisState as AnalysisState.Completed).result
                        notifications = notifications.map { n ->
                            if (n.id == notification.id) n.copy(
                                isAnalyzed = true,
                                riskLevel = result.riskLevel,
                                riskScore = result.riskScore
                            ) else n
                        }
                    }
                }

                val displayState = if (notification.isAnalyzed && analysisState is AnalysisState.Loading) {
                    AnalysisState.Completed(
                        AnalysisResult(
                            riskLevel = notification.riskLevel,
                            riskScore = notification.riskScore,
                            recommendation = getRecommendationForLevel(notification.riskLevel),
                            canAccess = notification.riskLevel == RiskLevel.SAFE,
                            details = null
                        )
                    )
                } else analysisState

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
                        if (UrlBlockManager.isBlocked(context, notification.url)) {
                            UrlBlockManager.unblockUrl(context, notification.url)
                            Toast.makeText(context, "차단이 해제되었습니다", Toast.LENGTH_SHORT).show()
                        } else {
                            UrlBlockManager.blockUrl(context, notification.url)
                            Toast.makeText(context, "URL이 차단되었습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        composable(Screen.UrlInput.route) {
            val analysisState by analysisViewModel.analysisState.collectAsState()
            val isLoading by analysisViewModel.isLoading.collectAsState()
            UrlInputScreen(
                analysisState = if (analysisState is AnalysisState.Loading && !isLoading) null else analysisState,
                isLoading = isLoading,
                onBackClick = {
                    analysisViewModel.resetAnalysis()
                    navController.popBackStack()
                },
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
            SettingsScreen(
                settingsState = settingsState,
                onBackClick = { navController.popBackStack() },
                onProfileEditClick = {},
                onSettingChange = { settingsState = it },
                onVersionClick = {},
                onTermsClick = {},
                onPrivacyClick = {},
                onOpenSourceClick = {},
                onResetOnboardingClick = {
                    context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("onboarding_completed", false)
                        .apply()
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogoutClick = {}
            )
        }
    }
}

@Composable
fun NotificationListScreenWithFab(
    notifications: List<NotificationItem>,
    unanalyzedCount: Int,
    onNotificationClick: (NotificationItem) -> Unit,
    onSettingsClick: () -> Unit,
    onUrlInputClick: () -> Unit,
    onQrScanClick: () -> Unit
) {
    var showFabMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        floatingActionButton = {
            ExpandableFab(
                expanded = showFabMenu,
                onExpandedChange = { showFabMenu = it },
                onUrlInputClick = {
                    showFabMenu = false
                    onUrlInputClick()
                },
                onQrScanClick = {
                    showFabMenu = false
                    onQrScanClick()
                }
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
            item {
                Column {
                    Text(
                        text = "알림 목록",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (unanalyzedCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${unanalyzedCount}개의 미분석 알림",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            if (notifications.isEmpty()) {
                item { EmptyNotificationView() }
            } else {
                items(notifications.size, { notifications[it].id }) { index ->
                    NotificationCard(
                        item = notifications[index],
                        onClick = { onNotificationClick(notifications[index]) }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
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
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtendedFloatingActionButton(
                    onClick = onQrScanClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("QR 스캔")
                }
                ExtendedFloatingActionButton(
                    onClick = onUrlInputClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("URL 입력")
                }
            }
        }
        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            containerColor = Primary
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "닫기" else "URL 검사",
                tint = Color.White
            )
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

private suspend fun updateStepsAndAnalyze(
    viewModel: UrlAnalysisViewModel,
    url: String,
    onStepUpdate: (List<AnalysisStepState>) -> Unit
) {
    val steps = createInitialAnalysisSteps().toMutableList()
    
    steps[0] = steps[0].copy(status = StepStatus.LOADING)
    onStepUpdate(steps.toList())
    kotlinx.coroutines.delay(300)
    steps[0] = steps[0].copy(status = StepStatus.COMPLETED)
    onStepUpdate(steps.toList())
    
    steps[1] = steps[1].copy(status = StepStatus.LOADING)
    onStepUpdate(steps.toList())
    kotlinx.coroutines.delay(200)
    steps[1] = steps[1].copy(status = StepStatus.COMPLETED)
    onStepUpdate(steps.toList())
    
    steps[2] = steps[2].copy(status = StepStatus.LOADING)
    onStepUpdate(steps.toList())
    steps[3] = steps[3].copy(status = StepStatus.LOADING)
    onStepUpdate(steps.toList())
    
    viewModel.analyzeUrl(url, "notification", true)
    
    var attempts = 0
    while (viewModel.isLoading.value && attempts < 60) {
        kotlinx.coroutines.delay(500)
        attempts++
    }
    
    steps[2] = steps[2].copy(status = StepStatus.COMPLETED)
    steps[3] = steps[3].copy(status = StepStatus.COMPLETED)
    onStepUpdate(steps.toList())
    
    steps[4] = steps[4].copy(
        status = if (viewModel.aiResponse.value != null) StepStatus.COMPLETED else StepStatus.SKIPPED
    )
    onStepUpdate(steps.toList())
}

private fun getRecommendationForLevel(riskLevel: RiskLevel) = when (riskLevel) {
    RiskLevel.SAFE -> "안전한 링크입니다. 정상적으로 접속하셔도 됩니다."
    RiskLevel.CAUTION -> "주의가 필요한 링크입니다. 개인정보 입력에 주의하세요."
    RiskLevel.DANGER -> "위험한 링크입니다. 접속을 권장하지 않습니다."
    RiskLevel.UNKNOWN -> "분석 중입니다."
}

val sampleNotifications = listOf(
    NotificationItem(
        id = "1",
        source = NotificationSource.KAKAOTALK,
        title = "새 메시지",
        content = "안녕하세요! 이벤트 참여하시면 경품을 받으실 수 있습니다. https://kakao-event-2024.com/win",
        url = "https://kakao-event-2024.com/win",
        timeAgo = "방금 전",
        riskLevel = RiskLevel.UNKNOWN,
        riskScore = 0,
        isAnalyzed = false
    ),
    NotificationItem(
        id = "2",
        source = NotificationSource.SMS,
        title = "[Web발신]",
        content = "[국민은행] 고객님의 보안카드가 만료되었습니다. 아래 링크에서 갱신하세요: http://kbbank-secure.info/renew",
        url = "http://kbbank-secure.info/renew",
        timeAgo = "5분 전",
        riskLevel = RiskLevel.UNKNOWN,
        riskScore = 0,
        isAnalyzed = false
    ),
    NotificationItem(
        id = "3",
        source = NotificationSource.GMAIL,
        title = "Your Amazon Order",
        content = "Your package has been delivered. Track your order: https://www.amazon.com/track/order/12345",
        url = "https://www.amazon.com/track/order/12345",
        timeAgo = "1시간 전",
        riskLevel = RiskLevel.UNKNOWN,
        riskScore = 0,
        isAnalyzed = false
    ),
    NotificationItem(
        id = "4",
        source = NotificationSource.SMS,
        title = "[Web발신]",
        content = "택배가 도착했습니다. 확인하세요: http://cjlogistics-kr.co/track/9283746",
        url = "http://cjlogistics-kr.co/track/9283746",
        timeAgo = "2시간 전",
        riskLevel = RiskLevel.UNKNOWN,
        riskScore = 0,
        isAnalyzed = false
    ),
    NotificationItem(
        id = "5",
        source = NotificationSource.KAKAOTALK,
        title = "친구 메시지",
        content = "이거 진짜 좋은 이벤트야! 빨리 신청해 https://naver-event.co.kr/free-gift",
        url = "https://naver-event.co.kr/free-gift",
        timeAgo = "3시간 전",
        riskLevel = RiskLevel.UNKNOWN,
        riskScore = 0,
        isAnalyzed = false
    )
)
