package com.example.myapplication.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.screens.detail.*
import com.example.myapplication.ui.screens.main.*
import com.example.myapplication.ui.screens.onboarding.*
import com.example.myapplication.ui.screens.settings.*

/**
 * 네비게이션 라우트 정의
 */
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object NotificationList : Screen("notification_list")
    object AnalysisDetail : Screen("analysis_detail/{notificationId}") {
        fun createRoute(notificationId: String) = "analysis_detail/$notificationId"
    }
    object Settings : Screen("settings")
}

/**
 * 앱 네비게이션 그래프
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Onboarding.route
) {
    // 샘플 데이터 (실제로는 ViewModel에서 관리)
    var notifications by remember {
        mutableStateOf(sampleNotifications)
    }

    var settingsState by remember {
        mutableStateOf(SettingsState())
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 온보딩 화면
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

        // 알림 목록 화면
        composable(Screen.NotificationList.route) {
            NotificationListScreen(
                notifications = notifications,
                unanalyzedCount = notifications.count { !it.isAnalyzed },
                onNotificationClick = { notification ->
                    navController.navigate(Screen.AnalysisDetail.createRoute(notification.id))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 분석 상세 화면
        composable(
            route = Screen.AnalysisDetail.route,
            arguments = listOf(
                navArgument("notificationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val notificationId = backStackEntry.arguments?.getString("notificationId") ?: ""
            val notification = notifications.find { it.id == notificationId }

            if (notification != null) {
                // 분석 상태 시뮬레이션
                var analysisState by remember { mutableStateOf<AnalysisState>(AnalysisState.Loading) }
                var analysisSteps by remember {
                    mutableStateOf(
                        listOf(
                            AnalysisStepState(AnalysisStep.URL_EXTRACT, StepStatus.LOADING),
                            AnalysisStepState(AnalysisStep.LOCAL_ML, StepStatus.PENDING),
                            AnalysisStepState(AnalysisStep.CLOUD_FUNCTION, StepStatus.PENDING),
                            AnalysisStepState(AnalysisStep.VIRUS_TOTAL, StepStatus.PENDING),
                            AnalysisStepState(AnalysisStep.OPENAI, StepStatus.PENDING)
                        )
                    )
                }

                // 분석 완료된 항목은 바로 결과 표시
                LaunchedEffect(notification.isAnalyzed) {
                    if (notification.isAnalyzed) {
                        analysisState = AnalysisState.Completed(
                            AnalysisResult(
                                riskLevel = notification.riskLevel,
                                riskScore = notification.riskScore,
                                recommendation = when (notification.riskLevel) {
                                    RiskLevel.SAFE -> "안전한 링크입니다. 정상적으로 접속하셔도 됩니다."
                                    RiskLevel.CAUTION -> "주의가 필요한 링크입니다. 개인정보 입력에 주의하세요."
                                    RiskLevel.DANGER -> "위험한 링크입니다. 접속을 권장하지 않습니다."
                                    RiskLevel.UNKNOWN -> "분석 중입니다."
                                },
                                canAccess = notification.riskLevel == RiskLevel.SAFE,
                                details = AnalysisDetails(
                                    localMlScore = notification.riskScore / 100f,
                                    virusTotalMalicious = if (notification.riskLevel == RiskLevel.DANGER) 3 else 0,
                                    virusTotalSuspicious = if (notification.riskLevel == RiskLevel.CAUTION) 2 else 0,
                                    redirectCount = 1
                                )
                            )
                        )
                    }
                }

                AnalysisDetailScreen(
                    notification = notification,
                    analysisState = analysisState,
                    analysisSteps = analysisSteps,
                    onBackClick = { navController.popBackStack() },
                    onShareClick = { /* 공유 기능 */ },
                    onBlockClick = { /* 차단 기능 */ }
                )
            }
        }

        // 설정 화면
        composable(Screen.Settings.route) {
            val context = LocalContext.current
            
            SettingsScreen(
                settingsState = settingsState,
                onBackClick = { navController.popBackStack() },
                onProfileEditClick = { /* 프로필 수정 */ },
                onSettingChange = { newState -> settingsState = newState },
                onVersionClick = { /* 버전 정보 */ },
                onTermsClick = { /* 이용 약관 */ },
                onPrivacyClick = { /* 개인정보 처리방침 */ },
                onOpenSourceClick = { /* 오픈소스 라이선스 */ },
                onResetOnboardingClick = {
                    // SharedPreferences 초기화
                    context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("onboarding_completed", false)
                        .apply()
                    
                    // 온보딩 화면으로 이동
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLogoutClick = { /* 로그아웃 */ }
            )
        }
    }
}

/**
 * 샘플 알림 데이터
 */
val sampleNotifications = listOf(
    NotificationItem(
        id = "1",
        source = NotificationSource.KAKAOTALK,
        title = "새 메시지",
        content = "안녕하세요! 이벤트 참여하시면 경품을 받으실 수 있습니다....",
        url = "https://kakao-event-2...",
        timeAgo = "방금 전",
        riskLevel = RiskLevel.DANGER,
        riskScore = 85,
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
        riskScore = 0,
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
        riskScore = 15,
        isAnalyzed = true
    ),
    NotificationItem(
        id = "4",
        source = NotificationSource.SMS,
        title = "[Web발신]",
        content = "택배가 도착했습니다. 확인하세요: http://cjlogistics-...",
        url = "http://cjlogistics-kr.co...",
        timeAgo = "2시간 전",
        riskLevel = RiskLevel.CAUTION,
        riskScore = 45,
        isAnalyzed = true
    ),
    NotificationItem(
        id = "5",
        source = NotificationSource.KAKAOTALK,
        title = "친구 메시지",
        content = "이거 진짜 좋은 이벤트야! 빨리 신청해 https://naver-...",
        url = "https://naver-event.co....",
        timeAgo = "3시간 전",
        riskLevel = RiskLevel.UNKNOWN,
        riskScore = 0,
        isAnalyzed = false
    )
)
