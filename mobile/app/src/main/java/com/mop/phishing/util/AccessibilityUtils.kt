package com.mop.phishing.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.mop.phishing.MailNotificationListener

/**
 * 권한 관련 유틸리티
 * - 접근성 서비스 (URL 차단용)
 * - 오버레이 권한 (차단 화면 표시용)
 * - 알림 리스너 서비스 (알림 감지용)
 */
object AccessibilityUtils {

    data class PermissionStatus(
        val isAccessibilityEnabled: Boolean,
        val isOverlayEnabled: Boolean,
        val isNotificationListenerEnabled: Boolean
    ) {
        val allGranted: Boolean
            get() = isAccessibilityEnabled && isOverlayEnabled && isNotificationListenerEnabled
        
        val urlBlockingEnabled: Boolean
            get() = isAccessibilityEnabled && isOverlayEnabled
        
        val notificationDetectionEnabled: Boolean
            get() = isNotificationListenerEnabled
    }

    /**
     * 모든 권한 상태 확인
     */
    fun checkAllPermissions(context: Context): PermissionStatus {
        return PermissionStatus(
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context),
            isOverlayEnabled = canDrawOverlays(context),
            isNotificationListenerEnabled = isNotificationListenerEnabled(context)
        )
    }

    // ========== 접근성 서비스 (URL 차단용) ==========

    /**
     * 접근성 서비스 활성화 여부 확인
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, com.mop.phishing.service.UrlBlockerAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    /**
     * 접근성 설정 화면 열기
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== 오버레이 권한 (차단 화면 표시용) ==========

    /**
     * 오버레이 권한 확인
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 오버레이 권한 설정 화면 열기
     */
    fun openOverlayPermissionSettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== 알림 리스너 서비스 (알림 감지용) ==========

    /**
     * 알림 리스너 서비스 활성화 여부 확인
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, MailNotificationListener::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledListeners)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    /**
     * 알림 리스너 설정 화면 열기
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
