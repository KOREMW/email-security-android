package com.mop.phishing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.mop.phishing.auth.AuthViewModel
import com.mop.phishing.auth.AuthState
import com.mop.phishing.ui.navigation.AppNavigation
import com.mop.phishing.ui.navigation.Screen
import com.mop.phishing.ui.theme.EmailSecurityTheme

class MainActivityCompose : ComponentActivity() {

    // AuthViewModel 참조 (Google Sign-In 결과 처리용)
    private var authViewModelRef: AuthViewModel? = null

    // POST_NOTIFICATIONS 권한 요청 런처 (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 권한 결과 처리 (필요시 사용자에게 안내)
    }

    // Google Sign-In 결과 런처
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModelRef?.handleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge 디스플레이 활성화
        enableEdgeToEdge()

        // Android 13+ POST_NOTIFICATIONS 런타임 권한 요청
        requestNotificationPermissionIfNeeded()

        // 알림 리스너 권한 확인
        ensureNotificationListenerPermission()

        // 온보딩 완료 여부 확인 (SharedPreferences 사용)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        // Intent에서 알림 클릭 정보 추출
        val alertNotificationId = intent?.getStringExtra("notification_id")

        setContent {
            // SettingsViewModel에서 다크 모드 상태 가져오기
            val settingsViewModel: SettingsViewModel = viewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()

            // AuthViewModel
            val authViewModel: AuthViewModel = viewModel()
            val authState by authViewModel.authState.collectAsState()

            // AuthViewModel 참조 저장 (Sign-In 결과 처리용)
            LaunchedEffect(authViewModel) {
                authViewModelRef = authViewModel
            }

            EmailSecurityTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    // 로그인 성공 시 메인 화면으로 이동 및 온보딩 완료 처리
                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthState.Success -> {
                                Toast.makeText(this@MainActivityCompose, "로그인되었습니다", Toast.LENGTH_SHORT).show()
                                // 온보딩 완료 저장
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                // 메인 화면으로 이동
                                navController.navigate(Screen.NotificationList.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                                authViewModel.resetState()
                            }
                            is AuthState.Error -> {
                                Toast.makeText(this@MainActivityCompose, (authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                                authViewModel.resetState()
                            }
                            else -> {}
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        startDestination = if (isOnboardingCompleted) {
                            Screen.NotificationList.route
                        } else {
                            Screen.Onboarding.route
                        },
                        settingsViewModel = settingsViewModel,
                        authViewModel = authViewModel,
                        onGoogleSignInClick = {
                            val signInIntent = authViewModel.getGoogleSignInIntent()
                            googleSignInLauncher.launch(signInIntent)
                        }
                    )

                    // 알림 클릭으로 앱 열렸을 때 해당 분석 상세 화면으로 이동
                    LaunchedEffect(alertNotificationId) {
                        if (alertNotificationId != null && isOnboardingCompleted) {
                            navController.navigate(Screen.AnalysisDetail.createRoute(alertNotificationId))
                        }
                    }

                    // 온보딩 완료 시 저장
                    LaunchedEffect(navController) {
                        navController.addOnDestinationChangedListener { _, destination, _ ->
                            if (destination.route == Screen.NotificationList.route && !isOnboardingCompleted) {
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Android 13+ POST_NOTIFICATIONS 런타임 권한 요청
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 이미 권한 있음
                }
                else -> {
                    // 권한 요청
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    /**
     * 알림 리스너 권한 확인 및 요청
     * 온보딩 완료 후에만 권한 요청을 표시하고, 사용자에게 안내 후 설정으로 이동
     */
    private fun ensureNotificationListenerPermission() {
        // 온보딩 완료 여부 확인
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)
        
        // 온보딩이 완료되지 않았으면 권한 요청 스킵 (온보딩에서 처리)
        if (!isOnboardingCompleted) return
        
        if (!isNotificationListenerEnabled()) {
            // 이미 안내했는지 확인 (하루에 한 번만)
            val lastPromptTime = prefs.getLong("last_noti_permission_prompt", 0)
            val currentTime = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L
            
            if (currentTime - lastPromptTime > oneDayMillis) {
                prefs.edit().putLong("last_noti_permission_prompt", currentTime).apply()
                showNotificationListenerPermissionDialog()
            }
        }
    }
    
    /**
     * 알림 접근 권한 활성화 여부 확인
     */
    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        return enabledListeners.contains(packageName)
    }
    
    /**
     * 권한 요청 다이얼로그 표시
     */
    private fun showNotificationListenerPermissionDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("알림 접근 권한 필요")
            .setMessage("이메일 및 메시지의 악성 URL을 감지하려면 알림 접근 권한이 필요합니다.\n\n설정에서 '이메일 보안' 앱을 활성화해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                try {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "설정 화면을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("나중에", null)
            .show()
    }
}
