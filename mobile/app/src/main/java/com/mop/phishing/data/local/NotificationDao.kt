package com.mop.phishing.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 알림 데이터 접근 객체 (DAO)
 */
@Dao
interface NotificationDao {
    
    /**
     * 모든 알림 조회 (최신순)
     * Flow로 반환하여 실시간 업데이트 지원
     */
    @Query("SELECT * FROM notifications ORDER BY receivedAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    /**
     * 미분석 알림만 조회
     */
    @Query("SELECT * FROM notifications WHERE isAnalyzed = 0 ORDER BY receivedAt DESC")
    fun getUnanalyzedNotifications(): Flow<List<NotificationEntity>>
    
    /**
     * 미분석 알림 개수
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE isAnalyzed = 0")
    fun getUnanalyzedCount(): Flow<Int>
    
    /**
     * ID로 알림 조회
     */
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: String): NotificationEntity?
    
    /**
     * URL로 알림 조회
     */
    @Query("SELECT * FROM notifications WHERE url = :url ORDER BY receivedAt DESC LIMIT 1")
    suspend fun getNotificationByUrl(url: String): NotificationEntity?
    
    /**
     * 알림 삽입 (중복 시 교체)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)
    
    /**
     * 여러 알림 삽입
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)
    
    /**
     * 알림 업데이트
     */
    @Update
    suspend fun updateNotification(notification: NotificationEntity)
    
    /**
     * 분석 결과 업데이트 (전체 필드)
     */
    @Query("""
        UPDATE notifications 
        SET isAnalyzed = 1, 
            riskLevel = :riskLevel, 
            riskScore = :riskScore,
            localMlScore = :localMlScore,
            virusTotalMalicious = :vtMalicious,
            virusTotalSuspicious = :vtSuspicious,
            virusTotalTotal = :vtTotal,
            finalScore = :finalScore,
            aiExplanation = :aiExplanation,
            recommendation = :recommendation,
            canAccess = :canAccess
        WHERE id = :id
    """)
    suspend fun updateAnalysisResult(
        id: String,
        riskLevel: String,
        riskScore: Int,
        localMlScore: Float,
        vtMalicious: Int,
        vtSuspicious: Int,
        vtTotal: Int,
        finalScore: Int,
        aiExplanation: String?,
        recommendation: String?,
        canAccess: Boolean
    )
    
    /**
     * 알림 삭제
     */
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)
    
    /**
     * ID로 알림 삭제
     */
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: String)
    
    /**
     * 모든 알림 삭제
     */
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
    
    /**
     * 오래된 알림 삭제 (기본 30일)
     */
    @Query("DELETE FROM notifications WHERE receivedAt < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long)
    
    /**
     * 위험 알림만 조회
     */
    @Query("SELECT * FROM notifications WHERE riskLevel = 'DANGER' ORDER BY receivedAt DESC")
    fun getDangerousNotifications(): Flow<List<NotificationEntity>>
    
    /**
     * 알림 총 개수
     */
    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getTotalCount(): Int
    
    /**
     * 위험 알림 개수
     */
    @Query("SELECT COUNT(*) FROM notifications WHERE riskLevel = 'DANGER'")
    suspend fun getDangerousCount(): Int
}
