package com.mop.phishing.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database
 * 앱의 로컬 데이터베이스
 */
@Database(
    entities = [NotificationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun notificationDao(): NotificationDao
    
    companion object {
        private const val DATABASE_NAME = "phishing_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 싱글톤 인스턴스 반환
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
