package com.mop.phishing

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mop.phishing.data.local.NotificationRepository
import com.mop.phishing.ui.components.NotificationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 알림 목록 ViewModel
 * Repository에서 알림 데이터를 구독하여 UI에 전달
 */
class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "NotificationVM"
    }

    private val repository = NotificationRepository.getInstance(application)

    // ============================================================
    // State
    // ============================================================

    /**
     * 알림 목록 (실시간 업데이트)
     */
    val notifications: StateFlow<List<NotificationItem>> = repository.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 미분석 알림 개수
     */
    val unanalyzedCount: StateFlow<Int> = repository.getUnanalyzedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    /**
     * 로딩 상태
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러 메시지
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        Log.d(TAG, "NotificationViewModel 초기화")
        
        // 오래된 알림 정리 (30일 이전)
        viewModelScope.launch {
            try {
                repository.cleanupOldNotifications(30)
            } catch (e: Exception) {
                Log.w(TAG, "오래된 알림 정리 실패: ${e.message}")
            }
        }
    }

    // ============================================================
    // Actions
    // ============================================================

    /**
     * 알림 삭제
     */
    fun deleteNotification(id: String) {
        viewModelScope.launch {
            try {
                repository.deleteNotification(id)
                Log.d(TAG, "알림 삭제: $id")
            } catch (e: Exception) {
                _errorMessage.value = "삭제 실패: ${e.message}"
            }
        }
    }

    /**
     * 모든 알림 삭제
     */
    fun deleteAllNotifications() {
        viewModelScope.launch {
            try {
                repository.deleteAllNotifications()
                Log.d(TAG, "모든 알림 삭제")
            } catch (e: Exception) {
                _errorMessage.value = "삭제 실패: ${e.message}"
            }
        }
    }

    /**
     * 에러 메시지 초기화
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 통계 조회
     */
    suspend fun getStats(): NotificationStats {
        return NotificationStats(
            total = repository.getTotalCount(),
            dangerous = repository.getDangerousCount()
        )
    }
}

/**
 * 알림 통계 데이터
 */
data class NotificationStats(
    val total: Int,
    val dangerous: Int
)
