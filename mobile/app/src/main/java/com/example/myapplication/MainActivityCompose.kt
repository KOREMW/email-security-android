package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ml.UrlClassifier
import com.example.myapplication.ui.navigation.AppNavigation
import com.example.myapplication.ui.navigation.Screen
import com.example.myapplication.ui.theme.EmailSecurityTheme

class MainActivityCompose : ComponentActivity() {

    private lateinit var classifier: UrlClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge 디스플레이 활성화
        enableEdgeToEdge()

        // ML 분류기 초기화
        classifier = UrlClassifier(this)

        // 알림 리스너 권한 확인
        ensureNotificationPermission()

        // 온보딩 완료 여부 확인 (SharedPreferences 사용)
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isOnboardingCompleted = prefs.getBoolean("onboarding_completed", false)

        setContent {
            // 다크 모드 상태 (실제로는 DataStore에서 가져옴)
            var isDarkMode by remember { mutableStateOf(false) }

            EmailSecurityTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    AppNavigation(
                        navController = navController,
                        startDestination = if (isOnboardingCompleted) {
                            Screen.NotificationList.route
                        } else {
                            Screen.Onboarding.route
                        }
                    )

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
     * 알림 리스너 권한 확인 및 요청
     */
    private fun ensureNotificationPermission() {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!enabledListeners.contains(packageName)) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::classifier.isInitialized) {
            classifier.close()
        }
    }
}
