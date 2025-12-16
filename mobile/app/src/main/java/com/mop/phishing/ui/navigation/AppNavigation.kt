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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.mop.phishing.ui.theme.SafeGreen
import com.mop.phishing.ui.theme.WarningYellow
import com.mop.phishing.util.AccessibilityUtils
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
            var isNotificationListenerEnabled by remember {
                mutableStateOf(AccessibilityUtils.isNotificationListenerEnabled(context))
            }

            // 화면 돌아올 때 권한 상태 갱신
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                        isOverlayEnabled = AccessibilityUtils.canDrawOverlays(context)
                        isNotificationListenerEnabled = AccessibilityUtils.isNotificationListenerEnabled(context)
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
                isNotificationListenerEnabled = isNotificationListenerEnabled,
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
                onNotificationListenerClick = {
                    Toast.makeText(context, "알림 접근 설정에서 '피싱 탐지'를 활성화해주세요", Toast.LENGTH_LONG).show()
                    AccessibilityUtils.openNotificationListenerSettings(context)
                },
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
                var isReanalyzing by remember { mutableStateOf(false) }

                LaunchedEffect(notification.id) {
                    analysisViewModel.analyzeNotification(
                        notificationId = notification.id,
                        url = notification.url,
                        source = "notification"
                    )
                }
                
                // 재분석 완료 시 로딩 상태 해제
                LaunchedEffect(analysisState) {
                    if (analysisState is AnalysisState.Completed || analysisState is AnalysisState.Error) {
                        isReanalyzing = false
                    }
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
                    onReanalyzeClick = {
                        // 전체 재분석: 온디바이스 ML + VirusTotal + AI 분석 (forceRefresh = true)
                        isReanalyzing = true
                        Toast.makeText(context, "🔄 전체 재분석 중... (ML + VirusTotal + AI)", Toast.LENGTH_SHORT).show()
                        analysisViewModel.analyzeNotification(
                            notificationId = notification.id,
                            url = notification.url,
                            source = "notification",
                            forceRefresh = true  // 캐시 무시하고 새로 분석
                        )
                    },
                    isBlocked = isBlocked,
                    isReanalyzing = isReanalyzing
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
            val currentUrl by analysisViewModel.currentUrl.collectAsState()
            UrlInputScreen(
                analysisState = if (analysisState is AnalysisState.Loading && !isLoading) null else analysisState,
                isLoading = isLoading,
                onBackClick = { analysisViewModel.resetAnalysis(); navController.popBackStack() },
                onAnalyzeClick = { url -> analysisViewModel.analyzeUrl(url, "manual", true) },
                // 🆕 재분석: 서버 캐시 무시하고 강제 재분석 (QR 스캔 URL도 지원)
                onReanalyzeClick = { inputUrl ->
                    val urlToAnalyze = inputUrl.ifBlank { currentUrl }
                    if (urlToAnalyze.isNotBlank()) {
                        analysisViewModel.analyzeUrl(urlToAnalyze, "manual", true, null, forceRefresh = true)
                    }
                }
            )
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onBackClick = { navController.popBackStack() },
                onUrlScanned = { url ->
                    analysisViewModel.resetAnalysis()
                    navController.popBackStack()
                    navController.navigate(Screen.UrlInput.route)
                    // 🔧 QR 스캔은 항상 forceRefresh=true로 VT API 강제 호출
                    // (서버 캐시에 VT 결과가 없는 경우 방지)
                    analysisViewModel.analyzeUrl(url, "qr", true, null, forceRefresh = true)
                }
            )
        }

        composable(Screen.Settings.route) {
            val lifecycleOwner = LocalLifecycleOwner.current

            var isAccessibilityEnabled by remember { mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context)) }
            var isOverlayEnabled by remember { mutableStateOf(AccessibilityUtils.canDrawOverlays(context)) }
            var isNotificationListenerEnabled by remember { mutableStateOf(AccessibilityUtils.isNotificationListenerEnabled(context)) }

            // 차단된 URL 목록 (실시간 업데이트)
            var blockedUrls by remember { mutableStateOf(UrlBlockManager.getBlockedUrls(context)) }
            
            // 차단된 URL에 위험도 매핑
            val blockedUrlsWithRisk: Map<String, RiskLevel?> = blockedUrls.associateWith { url ->
                notifications.find { it.url == url }?.riskLevel
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
                        isOverlayEnabled = AccessibilityUtils.canDrawOverlays(context)
                        isNotificationListenerEnabled = AccessibilityUtils.isNotificationListenerEnabled(context)
                        blockedUrls = UrlBlockManager.getBlockedUrls(context)
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

            // 다이얼로그 상태
            var showOpenSourceDialog by remember { mutableStateOf(false) }
            var showTermsDialog by remember { mutableStateOf(false) }
            var showPrivacyDialog by remember { mutableStateOf(false) }

            // 이용약관 다이얼로그
            if (showTermsDialog) {
                Dialog(onDismissRequest = { showTermsDialog = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("이용약관", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.weight(1f, fill = false).heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("시행일: 2025년 12월 16일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("본 이용약관은 피싱 탐지 앱의 이용 조건 및 절차, 이용자와 서비스 제공자의 권리, 의무 및 책임사항을 규정합니다. 앱을 설치하거나 이용함으로써 본 약관에 동의하는 것으로 간주됩니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제1조 (목적)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("본 약관은 앱이 제공하는 피싱 URL 탐지 서비스의 이용과 관련하여 서비스 제공자와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제2조 (서비스의 내용)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("앱은 다음과 같은 서비스를 제공합니다:\n\n1. URL 위험도 분석: 사용자가 입력하거나 알림에서 감지된 URL의 피싱 위험도를 분석합니다.\n2. 실시간 알림 모니터링: 이메일, SMS, 메신저 등의 알림에서 URL을 자동으로 감지하고 분석합니다.\n3. QR 코드 스캔: QR 코드에 포함된 URL을 스캔하여 위험도를 분석합니다.\n4. 악성 URL 차단: 위험한 것으로 판단된 URL에 대한 접근을 차단합니다.\n5. AI 기반 보안 보고서: 분석 결과에 대한 상세 보고서를 제공합니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제3조 (이용자의 의무)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("이용자는 다음 사항을 준수해야 합니다:\n\n1. 앱을 불법적인 목적으로 사용하지 않습니다.\n2. 앱의 정상적인 운영을 방해하는 행위를 하지 않습니다.\n3. 타인의 개인정보를 무단으로 수집하거나 도용하지 않습니다.\n4. 앱을 역설계, 디컴파일, 또는 소스 코드를 추출하려는 시도를 하지 않습니다.\n5. 앱을 통해 얻은 정보를 상업적 목적으로 무단 사용하지 않습니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제4조 (권한 및 개인정보)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("[필수 권한]\n• 알림 접근 권한: 이메일, SMS, 메신저 알림에서 URL 감지\n• 접근성 서비스: 브라우저에서 위험 URL 감지 및 차단\n• 다른 앱 위에 표시: 위험 URL 접근 시 경고 화면 표시\n• 카메라: QR 코드 스캔\n• 인터넷: 서버 API 및 VirusTotal 연동\n\n[수집하는 정보]\n• 분석 요청된 URL\n• 분석 결과 및 위험도 점수\n• 앱 사용 로그 (오류 분석 목적)\n• 로그인 시 Google 계정 정보 (이메일, 이름)\n\n[정보의 이용]\n• URL 위험도 분석 및 결과 제공\n• 서비스 품질 개선\n• 클라우드 동기화 (로그인 사용자에 한함)\n• 보안 위협 데이터베이스 구축\n\n[정보의 보관 및 삭제]\n• 분석 기록은 기기 내에 저장되며, 사용자가 직접 삭제할 수 있습니다.\n• 서버에 저장된 데이터는 7일 후 자동 삭제됩니다.\n• 계정 삭제 시 모든 관련 데이터가 삭제됩니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제5조 (제3자 서비스)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("앱은 다음 제3자 서비스를 활용합니다:\n\n• VirusTotal: URL 악성 여부 검사\n• Google Firebase: 백엔드 서버 및 인증\n• OpenAI: AI 기반 보안 분석 보고서 생성\n\n각 서비스의 이용약관 및 개인정보 처리방침이 추가로 적용될 수 있습니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제6조 (면책 조항)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("[탐지 정확도]\n• 앱은 머신러닝 및 외부 API를 활용하여 URL 위험도를 분석하지만, 100% 정확한 탐지를 보장하지 않습니다.\n• 안전하다고 판정된 URL이 실제로는 악성일 수 있으며, 위험하다고 판정된 URL이 실제로는 안전할 수 있습니다.\n• 앱의 분석 결과는 참고 자료로만 활용하시고, 최종 판단은 이용자 본인의 책임입니다.\n\n[서비스 중단]\n• 서버 점검, 시스템 장애, 천재지변 등의 사유로 서비스가 일시 중단될 수 있습니다.\n• 외부 API(VirusTotal, OpenAI 등)의 장애로 인해 일부 기능이 제한될 수 있습니다.\n\n[손해 배상]\n• 앱 사용으로 인해 발생한 직접적, 간접적, 우발적, 특수적, 결과적 손해에 대해 서비스 제공자는 책임을 지지 않습니다.\n• 이용자가 앱의 분석 결과를 신뢰하여 발생한 피해에 대해 서비스 제공자는 책임을 지지 않습니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제7조 (지적재산권)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("1. 앱의 모든 콘텐츠, 디자인, 소스 코드, 로고 등에 대한 지적재산권은 서비스 제공자에게 있습니다.\n2. 이용자는 서비스 제공자의 사전 서면 동의 없이 앱의 콘텐츠를 복제, 배포, 수정, 2차 저작물 작성 등에 사용할 수 없습니다.\n3. 앱에 사용된 오픈소스 라이브러리는 각각의 라이선스를 따릅니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제8조 (서비스 변경 및 종료)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("1. 서비스 제공자는 운영상, 기술상의 필요에 따라 서비스의 전부 또는 일부를 변경하거나 종료할 수 있습니다.\n2. 서비스 변경 또는 종료 시, 앱 내 공지사항을 통해 사전에 안내합니다.\n3. 무료로 제공되는 서비스의 변경 또는 종료에 대해 서비스 제공자는 별도의 보상 의무를 지지 않습니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제9조 (약관의 변경)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("1. 서비스 제공자는 필요한 경우 관련 법령에 위배되지 않는 범위 내에서 약관을 변경할 수 있습니다.\n2. 약관이 변경될 경우, 변경 내용과 시행일을 앱 내 공지사항을 통해 최소 7일 전에 안내합니다.\n3. 변경된 약관의 시행일 이후에도 서비스를 계속 이용하는 경우, 변경된 약관에 동의한 것으로 간주합니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제10조 (분쟁 해결)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("1. 서비스 이용과 관련하여 분쟁이 발생한 경우, 서비스 제공자와 이용자는 원만한 해결을 위해 성실히 협의합니다.\n2. 협의가 이루어지지 않을 경우, 관할 법원은 서비스 제공자의 소재지를 관할하는 법원으로 합니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제11조 (연락처)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("서비스 이용에 관한 문의사항이 있으시면 아래로 연락해 주시기 바랍니다.\n\n[이메일]\n• mojungwon2@gmail.com\n• minwook2000@naver.com\n• lucy1789@naver.com\n\n[개발자]\n한성대학교 융합보안학과\n모정원, 엄민욱, 이가연", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(8.dp))
                                Text("부칙", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("1. 본 약관은 2025년 12월 16일부터 시행됩니다.\n2. 본 약관에 명시되지 않은 사항은 관련 법령 및 상관례에 따릅니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(8.dp))
                                Text("최종 수정일: 2025년 12월 16일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { showTermsDialog = false }, modifier = Modifier.align(Alignment.End)) { Text("확인") }
                        }
                    }
                }
            }

            // 개인정보 처리방침 다이얼로그
            if (showPrivacyDialog) {
                Dialog(onDismissRequest = { showPrivacyDialog = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("개인정보 처리방침", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Column(
                                modifier = Modifier.weight(1f, fill = false).heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("시행일: 2025년 12월 16일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("피싱 탐지 앱은 이용자의 개인정보를 중요시하며, 「개인정보 보호법」을 준수하고 있습니다. 본 개인정보 처리방침은 앱이 수집하는 개인정보의 항목, 수집 목적, 보유 기간, 제3자 제공 등에 관한 사항을 안내합니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제1조 (개인정보의 수집 항목)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("[필수 수집 항목]\n• 분석 요청 URL: 피싱 위험도 분석\n• 기기 식별자: 서비스 이용 기록 관리\n• 앱 사용 로그: 오류 분석 및 서비스 개선\n\n[선택 수집 항목 (Google 로그인 시)]\n• 이메일 주소: 계정 식별 및 로그인\n• 이름 (닉네임): 앱 내 사용자 표시\n• 프로필 사진: 앱 내 프로필 표시\n\n[자동 수집 항목]\n• IP 주소: 보안 및 부정 이용 방지\n• 접속 시간: 서비스 이용 통계\n• 기기 정보 (OS 버전, 앱 버전): 서비스 호환성 유지", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제2조 (개인정보의 수집 방법)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("1. 사용자 직접 입력: URL 직접 입력, Google 로그인\n2. 자동 수집: 알림 모니터링을 통한 URL 감지, 앱 사용 로그\n3. 제3자 제공: Google 계정 연동 시 Google로부터 제공", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제3조 (개인정보의 이용 목적)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("[서비스 제공]\n• URL 피싱 위험도 분석 및 결과 제공\n• 실시간 알림 모니터링 및 위험 URL 차단\n• QR 코드 스캔 및 분석\n• AI 기반 보안 보고서 생성\n\n[서비스 개선]\n• 서비스 품질 향상 및 신규 기능 개발\n• 오류 분석 및 버그 수정\n• 이용자 통계 분석\n\n[보안]\n• 부정 이용 방지 및 탐지\n• 보안 위협 데이터베이스 구축\n• 피싱 패턴 학습 및 탐지 정확도 향상\n\n[클라우드 동기화 (로그인 사용자)]\n• 분석 기록 백업 및 복원\n• 차단 URL 목록 동기화\n• 다중 기기 간 설정 동기화", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제4조 (개인정보의 보유 및 이용 기간)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("• 분석 기록 (기기 내): 사용자 삭제 시까지 (설정에서 직접 삭제 가능)\n• 분석 기록 (서버): 7일 후 자동 삭제\n• 로그인 정보: 로그아웃 또는 탈퇴 시까지 (탈퇴 시 즉시 삭제)\n• 서비스 이용 로그: 30일 후 자동 삭제\n• 오류 로그: 30일 후 자동 삭제\n\n[예외 사항]\n관련 법령에 따라 보존이 필요한 경우, 해당 법령에서 정한 기간 동안 보관합니다.\n• 통신비밀보호법: 서비스 이용 기록, 접속 로그 - 3개월\n• 전자상거래법: 계약 또는 청약철회 기록 - 5년", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제5조 (개인정보의 제3자 제공)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("앱은 원칙적으로 이용자의 개인정보를 제3자에게 제공하지 않습니다. 다만, 다음의 경우에는 예외로 합니다.\n\n[이용자 동의]\n이용자가 사전에 제3자 제공에 동의한 경우\n\n[법령에 의한 경우]\n• 수사 목적으로 법령에 정해진 절차에 따라 수사기관의 요구가 있는 경우\n• 법원의 재판업무 수행을 위해 필요한 경우\n• 기타 관련 법령에 의해 요구되는 경우\n\n[서비스 제공을 위한 업무 위탁]\n• Google Firebase: 서버 호스팅, 인증 (제공 정보: 이메일, 분석 기록)\n• VirusTotal: URL 악성 여부 검사 (제공 정보: URL)\n• OpenAI: AI 분석 보고서 생성 (제공 정보: URL, 분석 결과)", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제6조 (개인정보의 파기)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("[파기 절차]\n이용 목적이 달성된 개인정보는 별도의 DB로 옮겨져 내부 방침에 따라 일정 기간 저장 후 파기됩니다.\n\n[파기 방법]\n• 전자적 파일: 복구 불가능한 방법으로 영구 삭제\n• 종이 문서: 분쇄기로 분쇄하거나 소각\n\n[사용자 직접 삭제]\n• 앱 설정 > 데이터 관리 > '분석 기록 삭제'를 통해 직접 삭제 가능\n• 로그아웃 시 기기 내 계정 정보 삭제\n• 앱 삭제 시 기기 내 모든 데이터 삭제", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제7조 (이용자의 권리와 행사 방법)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("이용자는 언제든지 다음과 같은 권리를 행사할 수 있습니다:\n\n• 개인정보 열람 요구\n• 개인정보 정정 요구\n• 개인정보 삭제 요구\n• 처리 정지 요구\n\n[권리 행사 방법]\n• 앱 내 설정: 분석 기록 삭제, 로그아웃\n• 이메일 문의", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제8조 (개인정보의 안전성 확보 조치)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("[기술적 조치]\n• 개인정보의 암호화 전송 (HTTPS/TLS)\n• 비밀번호 등 중요 정보의 암호화 저장\n• 해킹 등에 대비한 보안 시스템 구축\n• 접근 권한 관리 및 접근 통제\n\n[관리적 조치]\n• 개인정보 취급자의 최소화\n• 개인정보 취급자에 대한 교육\n• 내부 관리계획 수립 및 시행\n\n[물리적 조치]\n• 서버는 Google Cloud Platform(Firebase)에서 안전하게 관리\n• 물리적 접근 통제", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제9조 (쿠키 및 자동 수집 장치)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("앱은 웹 기반 서비스가 아니므로 쿠키를 사용하지 않습니다. 다만, 서비스 개선을 위해 다음과 같은 정보가 자동으로 수집될 수 있습니다:\n\n• 앱 버전 정보\n• 기기 OS 버전\n• 앱 사용 패턴 (익명화된 통계)\n• 오류 발생 로그", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제10조 (개인정보 보호책임자)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("[개인정보 보호책임자]\n소속: 한성대학교 융합보안학과\n\n이메일:\n• mojungwon2@gmail.com\n• minwook2000@naver.com\n• lucy1789@naver.com\n\n[개인정보 침해 신고 및 상담]\n• 개인정보침해신고센터: privacy.kisa.or.kr / 국번없이 118\n• 개인정보분쟁조정위원회: kopico.go.kr / 1833-6972\n• 대검찰청 사이버수사과: spo.go.kr / 국번없이 1301\n• 경찰청 사이버안전국: cyberbureau.police.go.kr / 국번없이 182", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제11조 (개인정보 처리방침의 변경)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("본 개인정보 처리방침은 법령, 정책 또는 보안 기술의 변경에 따라 내용이 추가, 삭제 및 수정될 수 있습니다.\n\n변경 시에는 다음과 같이 안내합니다:\n• 시행 7일 전: 앱 내 공지사항을 통해 변경 내용 안내\n• 중요 변경 시: 앱 실행 시 팝업으로 안내", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(4.dp))
                                Text("제12조 (적용 범위)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("본 개인정보 처리방침은 앱에만 적용됩니다. 앱에서 링크를 통해 이동하는 외부 사이트에 대해서는 해당 사이트의 개인정보 처리방침이 적용됩니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(8.dp))
                                Text("부칙", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("1. 본 개인정보 처리방침은 2025년 12월 16일부터 시행됩니다.\n2. 이전 개인정보 처리방침은 본 방침으로 대체됩니다.", style = MaterialTheme.typography.bodyMedium)
                                
                                Spacer(Modifier.height(8.dp))
                                Text("최종 수정일: 2025년 12월 16일", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { showPrivacyDialog = false }, modifier = Modifier.align(Alignment.End)) { Text("확인") }
                        }
                    }
                }
            }

            // 오픈소스 라이선스 다이얼로그
            if (showOpenSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showOpenSourceDialog = false },
                    title = {
                        Text(
                            text = "오픈소스 라이선스",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "이 앱은 다음 오픈소스 라이브러리를 사용합니다:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("• TensorFlow Lite - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• Retrofit2 - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• OkHttp3 - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• Jetpack Compose - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• Room Database - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• Firebase SDK - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• ML Kit - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• CameraX - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• Kotlin Coroutines - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• Material Design 3 - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• DataStore - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Text("• Navigation Compose - Apache 2.0", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "모든 라이브러리는 Apache License 2.0에 따라 사용됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showOpenSourceDialog = false }) {
                            Text("확인")
                        }
                    }
                )
            }

            SettingsScreen(
                settingsState = settingsState,
                isLoggedIn = isLoggedIn,
                isSyncing = isSyncing,
                lastSyncMessage = lastSyncMessage,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isOverlayEnabled = isOverlayEnabled,
                isNotificationListenerEnabled = isNotificationListenerEnabled,
                onBackClick = { navController.popBackStack() },
                onLoginClick = onGoogleSignInClick,
                onLogoutClick = { if (isLoggedIn) { authViewModel.signOut(); Toast.makeText(context, "로그아웃되었습니다", Toast.LENGTH_SHORT).show() } },
                onSyncClick = { authViewModel.syncData(); Toast.makeText(context, "동기화 시작...", Toast.LENGTH_SHORT).show() },
                onAccessibilityClick = { Toast.makeText(context, "접근성 설정에서 '피싱 URL 차단 서비스'를 활성화해주세요", Toast.LENGTH_LONG).show(); AccessibilityUtils.openAccessibilitySettings(context) },
                onOverlayPermissionClick = { Toast.makeText(context, "다른 앱 위에 표시 권한을 활성화해주세요", Toast.LENGTH_LONG).show(); AccessibilityUtils.openOverlayPermissionSettings(context) },
                onNotificationListenerClick = { Toast.makeText(context, "알림 접근 설정에서 '피싱 탐지'를 활성화해주세요", Toast.LENGTH_LONG).show(); AccessibilityUtils.openNotificationListenerSettings(context) },
                onSettingChange = { newState ->
                    if (newState.darkMode != settingsState.darkMode) settingsViewModel.updateDarkMode(newState.darkMode)
                    if (newState.autoAnalysis != settingsState.autoAnalysis) settingsViewModel.updateAutoAnalysis(newState.autoAnalysis)
                    if (newState.autoBlockHighRisk != settingsState.autoBlockHighRisk) settingsViewModel.updateAutoBlockHighRisk(newState.autoBlockHighRisk)
                    if (newState.pushNotification != settingsState.pushNotification) settingsViewModel.updatePushNotification(newState.pushNotification)
                    if (newState.soundNotification != settingsState.soundNotification) settingsViewModel.updateSoundNotification(newState.soundNotification)
                    if (newState.vibration != settingsState.vibration) settingsViewModel.updateVibration(newState.vibration)
                },
                onVersionClick = { Toast.makeText(context, "Email Security v1.0.0\nPowered by ML & Cloud Functions", Toast.LENGTH_LONG).show() },
                onTermsClick = { showTermsDialog = true },
                onPrivacyClick = { showPrivacyDialog = true },
                onOpenSourceClick = { showOpenSourceDialog = true },
                onResetOnboardingClick = { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("onboarding_completed", false).apply(); navController.navigate(Screen.Onboarding.route) { popUpTo(0) { inclusive = true } } },
                onClearHistoryClick = { notificationViewModel.deleteAllNotifications(); Toast.makeText(context, "분석 기록이 삭제되었습니다", Toast.LENGTH_SHORT).show() },
                blockedUrlsWithRisk = blockedUrlsWithRisk,
                onUnblockUrl = { url ->
                    UrlBlockManager.unblockUrl(context, url)
                    blockedUrls = UrlBlockManager.getBlockedUrls(context)
                    Toast.makeText(context, "차단이 해제되었습니다", Toast.LENGTH_SHORT).show()
                }
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
    isNotificationListenerEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    onNotificationClick: (NotificationItem) -> Unit,
    onSettingsClick: () -> Unit,
    onUrlInputClick: () -> Unit,
    onQrScanClick: () -> Unit,
    onNotificationListenerClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onOverlayPermissionClick: () -> Unit
) {
    var showFabMenu by remember { mutableStateOf(false) }
    // 배너 닫기 상태 (세션 동안만 유지)
    var isBannerDismissed by remember { mutableStateOf(false) }

    val needsPermissions = !isNotificationListenerEnabled || !isAccessibilityEnabled || !isOverlayEnabled
    
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
            
            // 🆕 권한 안내 배너 (3개 권한 통합)
            if (needsPermissions && !isBannerDismissed) {
                item {
                    PermissionSetupBanner(
                        isNotificationListenerEnabled = isNotificationListenerEnabled,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isOverlayEnabled = isOverlayEnabled,
                        onNotificationListenerClick = onNotificationListenerClick,
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
 * 🆕 권한 설정 안내 배너 (3개 권한 통합)
 */
@Composable
fun PermissionSetupBanner(
    isNotificationListenerEnabled: Boolean,
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    onNotificationListenerClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onOverlayClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val enabledCount = listOf(isNotificationListenerEnabled, isAccessibilityEnabled, isOverlayEnabled).count { it }
    val allEnabled = enabledCount == 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                allEnabled -> SafeGreen.copy(alpha = 0.15f)
                enabledCount == 0 -> DangerRed.copy(alpha = 0.15f)
                else -> WarningYellow.copy(alpha = 0.15f)
            }
        ),
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
                        tint = when {
                            allEnabled -> SafeGreen
                            enabledCount == 0 -> DangerRed
                            else -> WarningYellow
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "필수 권한 설정 ($enabledCount/3)",
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
                text = "알림 감지 및 URL 차단 기능을 사용하려면 아래 권한을 모두 활성화해주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            // 1. 알림 접근 권한
            PermissionButton(
                title = "1. 알림 접근 권한",
                description = "메일/문자/카카오톡 알림에서 URL 감지",
                isEnabled = isNotificationListenerEnabled,
                onClick = onNotificationListenerClick
            )

            Spacer(Modifier.height(8.dp))

            // 2. 접근성 서비스
            PermissionButton(
                title = "2. 접근성 서비스",
                description = "브라우저에서 위험 URL 감지",
                isEnabled = isAccessibilityEnabled,
                onClick = onAccessibilityClick
            )

            Spacer(Modifier.height(8.dp))

            // 3. 오버레이 권한
            PermissionButton(
                title = "3. 다른 앱 위에 표시",
                description = "위험 URL 차단 경고 화면 표시",
                isEnabled = isOverlayEnabled,
                onClick = onOverlayClick
            )

            // 모두 활성화되면 완료 메시지
            if (allEnabled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SafeGreen.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = SafeGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("모든 권한이 활성화되었습니다!", style = MaterialTheme.typography.bodyMedium, color = SafeGreen, fontWeight = FontWeight.SemiBold)
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
