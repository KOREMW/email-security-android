package com.mop.phishing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mop.phishing.data.local.SettingsData
import com.mop.phishing.data.local.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 설정 ViewModel
 * DataStore 기반 설정 관리 및 UI 상태 제공
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository.getInstance(application)

    /**
     * 전체 설정 상태 (실시간 업데이트)
     */
    val settings: StateFlow<SettingsData> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsData()
        )

    /**
     * 다크 모드 상태 (테마용)
     */
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // ============================================================
    // 설정 업데이트 메서드
    // ============================================================

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }

    fun updateAutoAnalysis(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoAnalysis(enabled)
        }
    }

    fun updateAutoBlockHighRisk(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoBlockHighRisk(enabled)
        }
    }

    fun updatePushNotification(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPushNotification(enabled)
        }
    }

    fun updateSoundNotification(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundNotification(enabled)
        }
    }

    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVibration(enabled)
        }
    }

    /**
     * 전체 설정 업데이트
     */
    fun updateAllSettings(settings: SettingsData) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }
}
