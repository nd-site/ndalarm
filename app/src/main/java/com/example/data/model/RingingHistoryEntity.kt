package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ringing_history")
data class RingingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alarmId: Long,
    val alarmLabel: String,
    val triggeredTime: Long = System.currentTimeMillis(),
    val unlockedTime: Long = 0L,
    val unlockMethod: String = "TOKEN_50", // "TOKEN_50", "PIN_8", "WEB_PORTAL", "MATH"
    val unlockedByNdid: String = "SELF"
)
