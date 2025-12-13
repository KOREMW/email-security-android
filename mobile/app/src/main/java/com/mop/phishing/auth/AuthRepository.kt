package com.mop.phishing.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * 인증 Repository
 * Google 로그인 + Firebase Auth 연동
 */
class AuthRepository(private val context: Context) {

    companion object {
        private const val TAG = "AuthRepository"

        // Firebase 콘솔에서 발급받은 Web Client ID
        // google-services.json의 oauth_client에서 client_type: 3 (Web)
        private const val WEB_CLIENT_ID = "473329976016-63hmgdlpl9k99rg0e6r8hoq3n7oumolv.apps.googleusercontent.com"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(context: Context): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                AuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Auth 상태 변경 리스너
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            _isLoggedIn.value = firebaseAuth.currentUser != null
            Log.d(TAG, "Auth state changed: ${firebaseAuth.currentUser?.email ?: "not logged in"}")
        }
    }

    /**
     * Google 로그인 Intent 반환
     */
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Google 로그인 결과 처리
     */
    suspend fun handleGoogleSignInResult(data: Intent?): Result<FirebaseUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign in failed: ${e.statusCode}", e)
            Result.failure(Exception("Google 로그인 실패: ${e.statusCode}"))
        }
    }

    /**
     * Google 계정으로 Firebase 인증
     */
    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Firebase auth success: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("사용자 정보를 가져올 수 없습니다"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase auth failed", e)
            Result.failure(e)
        }
    }

    /**
     * 로그아웃
     */
    suspend fun signOut() {
        try {
            // Firebase 로그아웃
            auth.signOut()
            // Google 로그아웃
            googleSignInClient.signOut().await()
            Log.d(TAG, "Sign out success")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
        }
    }

    /**
     * 계정 삭제
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            googleSignInClient.revokeAccess().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete account failed", e)
            Result.failure(e)
        }
    }

    /**
     * 현재 사용자 정보
     */
    fun getCurrentUserInfo(): UserInfo? {
        val user = auth.currentUser ?: return null
        return UserInfo(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: "사용자",
            photoUrl = user.photoUrl?.toString()
        )
    }
}

/**
 * 사용자 정보 데이터 클래스
 */
data class UserInfo(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?
)
