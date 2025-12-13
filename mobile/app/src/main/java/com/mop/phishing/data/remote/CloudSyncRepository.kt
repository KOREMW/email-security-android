package com.mop.phishing.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mop.phishing.data.local.NotificationEntity
import com.mop.phishing.data.local.NotificationRepository
import com.mop.phishing.util.UrlBlockManager
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.first

/**
 * 클라우드 동기화 Repository
 * Firebase Firestore를 통한 데이터 동기화
 */
class CloudSyncRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CloudSyncRepository"

        // Firestore 컬렉션 경로
        private const val USERS_COLLECTION = "users"
        private const val NOTIFICATIONS_COLLECTION = "notifications"
        private const val BLOCKED_URLS_DOC = "blocked_urls"

        @Volatile
        private var INSTANCE: CloudSyncRepository? = null

        fun getInstance(context: Context): CloudSyncRepository {
            return INSTANCE ?: synchronized(this) {
                CloudSyncRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val localRepository = NotificationRepository.getInstance(context)

    /**
     * 현재 로그인된 사용자 ID
     */
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Boolean = currentUserId != null

    // ============================================================
    // 분석 기록 동기화
    // ============================================================

    /**
     * 단일 알림을 클라우드에 저장
     */
    suspend fun syncNotificationToCloud(notification: NotificationEntity): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("로그인이 필요합니다"))

        return try {
            val docRef = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notification.id)

            val data = notification.toFirestoreMap()
            docRef.set(data, SetOptions.merge()).await()

            Log.d(TAG, "알림 클라우드 동기화 완료: ${notification.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "알림 클라우드 동기화 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 모든 로컬 알림을 클라우드에 업로드
     */
    suspend fun uploadAllNotificationsToCloud(): Result<Int> {
        val userId = currentUserId ?: return Result.failure(Exception("로그인이 필요합니다"))

        return try {
            val notifications = localRepository.getAllNotifications().first()
            var uploadedCount = 0

            val batch = firestore.batch()
            val collectionRef = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_COLLECTION)

            notifications.forEach { item ->
                val entity = item.toEntity()
                val docRef = collectionRef.document(entity.id)
                batch.set(docRef, entity.toFirestoreMap(), SetOptions.merge())
                uploadedCount++
            }

            if (uploadedCount > 0) {
                batch.commit().await()
            }

            Log.d(TAG, "전체 알림 업로드 완료: ${uploadedCount}개")
            Result.success(uploadedCount)
        } catch (e: Exception) {
            Log.e(TAG, "전체 알림 업로드 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 클라우드에서 알림 다운로드하여 로컬에 저장
     */
    suspend fun downloadNotificationsFromCloud(): Result<Int> {
        val userId = currentUserId ?: return Result.failure(Exception("로그인이 필요합니다"))

        return try {
            val snapshot = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_COLLECTION)
                .get()
                .await()

            var downloadedCount = 0

            snapshot.documents.forEach { doc ->
                val entity = doc.toNotificationEntity()
                if (entity != null) {
                    // 로컬에 없거나 클라우드 데이터가 더 최신이면 저장
                    val localEntity = localRepository.getNotificationById(entity.id)
                    if (localEntity == null || entity.receivedAt > localEntity.receivedAt) {
                        localRepository.insertNotification(entity)
                        downloadedCount++
                    }
                }
            }

            Log.d(TAG, "클라우드에서 알림 다운로드 완료: ${downloadedCount}개")
            Result.success(downloadedCount)
        } catch (e: Exception) {
            Log.e(TAG, "클라우드에서 알림 다운로드 실패", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 차단 URL 동기화
    // ============================================================

    /**
     * 차단 URL 목록을 클라우드에 업로드
     */
    suspend fun uploadBlockedUrlsToCloud(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("로그인이 필요합니다"))

        return try {
            val blockedUrls = UrlBlockManager.getBlockedUrls(context)
            val blockedDomains = UrlBlockManager.getBlockedDomains(context)

            val data = mapOf(
                "blocked_urls" to blockedUrls.toList(),
                "blocked_domains" to blockedDomains.toList(),
                "updated_at" to System.currentTimeMillis()
            )

            firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection("settings")
                .document(BLOCKED_URLS_DOC)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "차단 URL 업로드 완료: URLs=${blockedUrls.size}, Domains=${blockedDomains.size}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "차단 URL 업로드 실패", e)
            Result.failure(e)
        }
    }

    /**
     * 클라우드에서 차단 URL 다운로드
     */
    suspend fun downloadBlockedUrlsFromCloud(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("로그인이 필요합니다"))

        return try {
            val doc = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection("settings")
                .document(BLOCKED_URLS_DOC)
                .get()
                .await()

            if (doc.exists()) {
                @Suppress("UNCHECKED_CAST")
                val cloudUrls = doc.get("blocked_urls") as? List<String> ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val cloudDomains = doc.get("blocked_domains") as? List<String> ?: emptyList()

                // 클라우드 데이터와 로컬 데이터 병합
                val localUrls = UrlBlockManager.getBlockedUrls(context)
                val localDomains = UrlBlockManager.getBlockedDomains(context)

                // 새로운 URL만 추가
                cloudUrls.forEach { url ->
                    if (!localUrls.contains(url)) {
                        UrlBlockManager.blockUrl(context, url)
                    }
                }

                cloudDomains.forEach { domain ->
                    if (!localDomains.contains(domain)) {
                        UrlBlockManager.blockDomain(context, domain)
                    }
                }

                Log.d(TAG, "차단 URL 다운로드 완료")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "차단 URL 다운로드 실패", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 전체 동기화
    // ============================================================

    /**
     * 전체 동기화 (양방향)
     * 로그인 시 호출
     */
    suspend fun syncAll(): SyncResult {
        if (!isLoggedIn()) {
            return SyncResult(success = false, message = "로그인이 필요합니다")
        }

        var uploadedNotifications = 0
        var downloadedNotifications = 0
        val errors = mutableListOf<String>()

        // 1. 로컬 → 클라우드 (알림)
        uploadAllNotificationsToCloud().fold(
            onSuccess = { uploadedNotifications = it },
            onFailure = { errors.add("알림 업로드 실패: ${it.message}") }
        )

        // 2. 클라우드 → 로컬 (알림)
        downloadNotificationsFromCloud().fold(
            onSuccess = { downloadedNotifications = it },
            onFailure = { errors.add("알림 다운로드 실패: ${it.message}") }
        )

        // 3. 차단 URL 동기화
        uploadBlockedUrlsToCloud().fold(
            onSuccess = { },
            onFailure = { errors.add("차단 URL 업로드 실패: ${it.message}") }
        )

        downloadBlockedUrlsFromCloud().fold(
            onSuccess = { },
            onFailure = { errors.add("차단 URL 다운로드 실패: ${it.message}") }
        )

        val success = errors.isEmpty()
        val message = if (success) {
            "동기화 완료 (업로드: $uploadedNotifications, 다운로드: $downloadedNotifications)"
        } else {
            errors.joinToString("\n")
        }

        Log.d(TAG, "전체 동기화 완료: $message")

        return SyncResult(
            success = success,
            message = message,
            uploadedCount = uploadedNotifications,
            downloadedCount = downloadedNotifications
        )
    }

    /**
     * 클라우드 데이터 삭제 (계정 삭제 시)
     */
    suspend fun deleteAllCloudData(): Result<Unit> {
        val userId = currentUserId ?: return Result.failure(Exception("로그인이 필요합니다"))

        return try {
            // 알림 삭제
            val notificationsRef = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(NOTIFICATIONS_COLLECTION)

            val notifications = notificationsRef.get().await()
            val batch = firestore.batch()
            notifications.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()

            // 설정 삭제
            firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection("settings")
                .document(BLOCKED_URLS_DOC)
                .delete()
                .await()

            // 유저 문서 삭제
            firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            Log.d(TAG, "클라우드 데이터 삭제 완료")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "클라우드 데이터 삭제 실패", e)
            Result.failure(e)
        }
    }
}

/**
 * 동기화 결과
 */
data class SyncResult(
    val success: Boolean,
    val message: String,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0
)

// ============================================================
// 확장 함수
// ============================================================

/**
 * NotificationEntity → Firestore Map
 */
private fun NotificationEntity.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "source" to source,
        "title" to title,
        "content" to content,
        "url" to url,
        "receivedAt" to receivedAt,
        "isAnalyzed" to isAnalyzed,
        "riskLevel" to riskLevel,
        "riskScore" to riskScore,
        "localMlScore" to localMlScore,
        "virusTotalMalicious" to virusTotalMalicious,
        "virusTotalSuspicious" to virusTotalSuspicious,
        "aiExplanation" to aiExplanation,
        "sourcePackage" to sourcePackage
    )
}

/**
 * Firestore Document → NotificationEntity
 */
private fun com.google.firebase.firestore.DocumentSnapshot.toNotificationEntity(): NotificationEntity? {
    return try {
        NotificationEntity(
            id = getString("id") ?: return null,
            source = getString("source") ?: "UNKNOWN",
            title = getString("title") ?: "",
            content = getString("content") ?: "",
            url = getString("url") ?: "",
            receivedAt = getLong("receivedAt") ?: System.currentTimeMillis(),
            isAnalyzed = getBoolean("isAnalyzed") ?: false,
            riskLevel = getString("riskLevel") ?: "UNKNOWN",
            riskScore = getLong("riskScore")?.toInt() ?: 0,
            localMlScore = getDouble("localMlScore")?.toFloat() ?: 0f,
            virusTotalMalicious = getLong("virusTotalMalicious")?.toInt() ?: 0,
            virusTotalSuspicious = getLong("virusTotalSuspicious")?.toInt() ?: 0,
            aiExplanation = getString("aiExplanation"),
            sourcePackage = getString("sourcePackage")
        )
    } catch (e: Exception) {
        Log.e("CloudSync", "Entity 변환 실패", e)
        null
    }
}

/**
 * NotificationItem → NotificationEntity 변환
 */
private fun com.mop.phishing.ui.components.NotificationItem.toEntity(): NotificationEntity {
    return NotificationEntity(
        id = this.id,
        source = this.source.name,
        title = this.title,
        content = this.content,
        url = this.url,
        riskLevel = this.riskLevel.name,
        riskScore = this.riskScore,
        isAnalyzed = this.isAnalyzed
    )
}
