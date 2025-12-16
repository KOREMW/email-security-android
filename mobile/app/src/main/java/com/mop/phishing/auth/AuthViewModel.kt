package com.mop.phishing.auth

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.mop.phishing.data.remote.CloudSyncRepository
import com.mop.phishing.data.remote.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 인증 ViewModel
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val authRepository = AuthRepository.getInstance(application)
    private val cloudSyncRepository = CloudSyncRepository.getInstance(application)

    val currentUser: StateFlow<FirebaseUser?> = authRepository.currentUser
    val isLoggedIn: StateFlow<Boolean> = authRepository.isLoggedIn

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // 동기화 상태
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Google 로그인 Intent 가져오기
     */
    fun getGoogleSignInIntent(): Intent {
        _authState.value = AuthState.Loading
        return authRepository.getGoogleSignInIntent()
    }

    /**
     * Google 로그인 결과 처리
     */
    fun handleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.handleGoogleSignInResult(data)
            _authState.value = result.fold(
                onSuccess = { user ->
                    // 로그인 성공 시 자동 동기화
                    syncData()
                    AuthState.Success(user)
                },
                onFailure = { AuthState.Error(it.message ?: "로그인 실패") }
            )
        }
    }

    /**
     * 클라우드 데이터 동기화
     */
    fun syncData() {
        if (!cloudSyncRepository.isLoggedIn()) {
            _syncState.value = SyncState.Error("로그인이 필요합니다")
            return
        }

        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val result = cloudSyncRepository.syncAll()
            _syncState.value = if (result.success) {
                Log.d(TAG, "동기화 완료: ${result.message}")
                SyncState.Success(result)
            } else {
                Log.e(TAG, "동기화 실패: ${result.message}")
                SyncState.Error(result.message)
            }
        }
    }

    /**
     * 동기화 상태 초기화
     */
    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    /**
     * 로그아웃
     */
    fun signOut() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.signOut()
            _authState.value = AuthState.Idle
        }
    }

    /**
     * 현재 사용자 정보
     */
    fun getUserInfo(): UserInfo? = authRepository.getCurrentUserInfo()

    /**
     * 상태 초기화
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

/**
 * 인증 상태
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * 동기화 상태
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val result: SyncResult) : SyncState()
    data class Error(val message: String) : SyncState()
}
