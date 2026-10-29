package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AlarmDao
import com.example.data.dao.RingingHistoryDao
import com.example.data.dao.TrustedNdidDao
import com.example.data.model.AlarmEntity
import com.example.data.model.RingingHistoryEntity
import com.example.data.model.TrustedNdidEntity

@Database(
    entities = [
        AlarmEntity::class,
        TrustedNdidEntity::class,
        RingingHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NDAlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun trustedNdidDao(): TrustedNdidDao
    abstract fun ringingHistoryDao(): RingingHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: NDAlarmDatabase? = null

        fun getDatabase(context: Context): NDAlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NDAlarmDatabase::class.java,
                    "nd_alarm_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
