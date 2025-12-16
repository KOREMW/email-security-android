package com.mop.phishing.data.local

import android.content.Context
import android.util.Log
import com.mop.phishing.ui.components.NotificationItem
import com.mop.phishing.ui.components.NotificationSource
import com.mop.phishing.ui.components.RiskLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * 알림 데이터 Repository
 * DAO를 감싸서 비즈니스 로직 처리
 */
class NotificationRepository private constructor(context: Context) {
    
    companion object {
        private const val TAG = "NotificationRepo"
        
        @Volatile
        private var INSTANCE: NotificationRepository? = null
        
        fun getInstance(context: Context): NotificationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = NotificationRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val dao: NotificationDao = AppDatabase.getInstance(context).notificationDao()
    
    // ============================================================
    // 조회
    // ============================================================
    
    /**
     * 모든 알림 조회 (Flow - 실시간 업데이트)
     */
    fun getAllNotifications(): Flow<List<NotificationItem>> {
        return dao.getAllNotifications().map { entities ->
            entities.map { it.toNotificationItem() }
        }
    }
    
    /**
     * 미분석 알림 개수
     */
    fun getUnanalyzedCount(): Flow<Int> {
        return dao.getUnanalyzedCount()
    }
    
    /**
     * ID로 알림 조회
     */
    suspend fun getNotificationById(id: String): NotificationEntity? {
        return dao.getNotificationById(id)
    }

    /**
     * URL로 분석된 결과 조회 (캐싱)
     * 이미 분석된 URL이면 기존 결과 반환
     */
    suspend fun getAnalyzedResultByUrl(url: String): NotificationEntity? {
        val entity = dao.getNotificationByUrl(url)
        // 분석이 완료된 경우만 반환
        return if (entity?.isAnalyzed == true) entity else null
    }

    /**
     * URL이 이미 분석되었는지 확인
     */
    suspend fun isUrlAlreadyAnalyzed(url: String): Boolean {
        val entity = dao.getNotificationByUrl(url)
        return entity?.isAnalyzed == true
    }

    /**
     * ID로 분석된 결과 조회
     */
    suspend fun getAnalyzedResultById(id: String): NotificationEntity? {
        val entity = dao.getNotificationById(id)
        return if (entity?.isAnalyzed == true) entity else null
    }
    
    // ============================================================
    // 삽입/수정
    // ============================================================
    
    /**
     * 새 알림 저장
     * MailNotificationListener에서 호출
     */
    suspend fun saveNotification(
        source: NotificationSource,
        title: String,
        content: String,
        url: String,
        sourcePackage: String? = null
    ): String {
        val id = UUID.randomUUID().toString()
        
        val entity = NotificationEntity(
            id = id,
            source = source.name,
            title = title,
            content = content,
            url = url,
            sourcePackage = sourcePackage,
            receivedAt = System.currentTimeMillis(),
            isAnalyzed = false,
            riskLevel = RiskLevel.UNKNOWN.name,
            riskScore = 0
        )
        
        dao.insertNotification(entity)
        Log.d(TAG, "알림 저장 완료: id=$id, url=$url")
        
        return id
    }
    
    /**
     * 분석 결과 업데이트 (전체 필드)
     */
    suspend fun updateAnalysisResult(
        id: String,
        riskLevel: RiskLevel,
        riskScore: Int,
        localMlScore: Float = 0f,
        vtMalicious: Int = 0,
        vtSuspicious: Int = 0,
        vtTotal: Int = 0,
        finalScore: Int = 0,
        aiExplanation: String? = null,
        recommendation: String? = null,
        canAccess: Boolean = true
    ) {
        dao.updateAnalysisResult(
            id = id,
            riskLevel = riskLevel.name,
            riskScore = riskScore,
            localMlScore = localMlScore,
            vtMalicious = vtMalicious,
            vtSuspicious = vtSuspicious,
            vtTotal = vtTotal,
            finalScore = finalScore,
            aiExplanation = aiExplanation,
            recommendation = recommendation,
            canAccess = canAccess
        )
        Log.d(TAG, "분석 결과 업데이트: id=$id, level=$riskLevel, score=$riskScore, finalScore=$finalScore")
    }
    
    /**
     * 알림 엔티티 직접 삽입
     */
    suspend fun insertNotification(entity: NotificationEntity) {
        dao.insertNotification(entity)
    }
    
    // ============================================================
    // 삭제
    // ============================================================
    
    /**
     * 알림 삭제
     */
    suspend fun deleteNotification(id: String) {
        dao.deleteNotificationById(id)
        Log.d(TAG, "알림 삭제: id=$id")
    }
    
    /**
     * 모든 알림 삭제
     */
    suspend fun deleteAllNotifications() {
        dao.deleteAllNotifications()
        Log.d(TAG, "모든 알림 삭제")
    }
    
    /**
     * 오래된 알림 정리 (기본 30일)
     */
    suspend fun cleanupOldNotifications(daysOld: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        dao.deleteOldNotifications(cutoffTime)
        Log.d(TAG, "${daysOld}일 이전 알림 정리 완료")
    }
    
    // ============================================================
    // 통계
    // ============================================================
    
    /**
     * 총 알림 개수
     */
    suspend fun getTotalCount(): Int {
        return dao.getTotalCount()
    }
    
    /**
     * 위험 알림 개수
     */
    suspend fun getDangerousCount(): Int {
        return dao.getDangerousCount()
    }
}
