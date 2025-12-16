package com.mop.phishing.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * 설정 데이터 저장소
 * DataStore를 사용하여 앱 설정을 영구 저장
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        // 사용자 정보
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")

        // 보안 설정
        private val AUTO_ANALYSIS = booleanPreferencesKey("auto_analysis")
        private val AUTO_BLOCK_HIGH_RISK = booleanPreferencesKey("auto_block_high_risk")

        // 알림 설정
        private val PUSH_NOTIFICATION = booleanPreferencesKey("push_notification")
        private val SOUND_NOTIFICATION = booleanPreferencesKey("sound_notification")
        private val VIBRATION = booleanPreferencesKey("vibration")

        // 화면 설정
        private val DARK_MODE = booleanPreferencesKey("dark_mode")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // ============================================================
    // 다크 모드 (가장 중요 - 테마에 직접 영향)
    // ============================================================

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    // ============================================================
    // 전체 설정 Flow
    // ============================================================

    val settingsFlow: Flow<SettingsData> = context.dataStore.data.map { preferences ->
        SettingsData(
            userName = preferences[USER_NAME] ?: "사용자",
            userEmail = preferences[USER_EMAIL] ?: "user@example.com",
            autoAnalysis = preferences[AUTO_ANALYSIS] ?: true,
            autoBlockHighRisk = preferences[AUTO_BLOCK_HIGH_RISK] ?: false,
            pushNotification = preferences[PUSH_NOTIFICATION] ?: true,
            soundNotification = preferences[SOUND_NOTIFICATION] ?: true,
            vibration = preferences[VIBRATION] ?: true,
            darkMode = preferences[DARK_MODE] ?: false
        )
    }

    // ============================================================
    // 개별 설정 업데이트
    // ============================================================

    suspend fun updateSettings(settings: SettingsData) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = settings.userName
            preferences[USER_EMAIL] = settings.userEmail
            preferences[AUTO_ANALYSIS] = settings.autoAnalysis
            preferences[AUTO_BLOCK_HIGH_RISK] = settings.autoBlockHighRisk
            preferences[PUSH_NOTIFICATION] = settings.pushNotification
            preferences[SOUND_NOTIFICATION] = settings.soundNotification
            preferences[VIBRATION] = settings.vibration
            preferences[DARK_MODE] = settings.darkMode
        }
    }

    suspend fun setAutoAnalysis(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_ANALYSIS] = enabled
        }
    }

    suspend fun setAutoBlockHighRisk(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BLOCK_HIGH_RISK] = enabled
        }
    }

    suspend fun setPushNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PUSH_NOTIFICATION] = enabled
        }
    }

    suspend fun setSoundNotification(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_NOTIFICATION] = enabled
        }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION] = enabled
        }
    }

    // ============================================================
    // 동기 읽기 메서드 (Service에서 사용)
    // ============================================================

    /**
     * 자동 분석 설정 (동기)
     */
    fun isAutoAnalysisEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[AUTO_ANALYSIS] ?: true }.first()
    }

    /**
     * 고위험 자동 차단 설정 (동기)
     */
    fun isAutoBlockHighRiskEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[AUTO_BLOCK_HIGH_RISK] ?: false }.first()
    }

    /**
     * 푸시 알림 설정 (동기)
     */
    fun isPushNotificationEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[PUSH_NOTIFICATION] ?: true }.first()
    }

    /**
     * 소리 알림 설정 (동기)
     */
    fun isSoundNotificationEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[SOUND_NOTIFICATION] ?: true }.first()
    }

    /**
     * 진동 설정 (동기)
     */
    fun isVibrationEnabled(): Boolean = runBlocking {
        context.dataStore.data.map { it[VIBRATION] ?: true }.first()
    }
}

/**
 * 설정 데이터 클래스
 */
data class SettingsData(
    val userName: String = "사용자",
    val userEmail: String = "user@example.com",
    val autoAnalysis: Boolean = true,
    val autoBlockHighRisk: Boolean = false,
    val pushNotification: Boolean = true,
    val soundNotification: Boolean = true,
    val vibration: Boolean = true,
    val darkMode: Boolean = false
)
