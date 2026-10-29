package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String = "Báo thức ND",
    val hour: Int = 7,
    val minute: Int = 0,
    val isEnabled: Boolean = true,
    
    // Calendar type: false = Solar (Dương lịch), true = Lunar (Âm lịch)
    val isLunarCalendar: Boolean = false,
    
    // Recurring days: e.g. "DAILY", "MON,TUE,WED,THU,FRI", "ONCE", "LUNAR_1_15"
    val repeatRule: String = "DAILY",
    
    // Specific lunar targeting
    val lunarDay: Int = 1, // 1 = Mùng 1, 15 = Rằm, or custom lunar day
    val lunarMonth: Int = 0, // 0 = every lunar month, 1-12 = specific month (e.g. Giỗ/Tết)
    
    // Sound & Vibration
    val ringtoneType: String = "gentle_breeze", // gentle_breeze, digital_pulse, morning_ocean, synth_melody, system
    val vibrate: Boolean = true,
    val volume: Float = 1.0f,
    
    // Anti-Cheat & ND Labs Ecosystem Security
    val antiCheatMode: String = "TOKEN_50", // TOKEN_50, PIN_8, WEB_SYNC, MATH_CHALLENGE
    val secretToken50: String = "", // 50-character random token for web/autofill unlock
    val emergencyPin8: String = "", // 8-digit PIN for remote / cross-device unlock
    val clockId: String = "ND-CL01", // 6-character unique clock identifier
    val ndid: String = "ND-MASTER", // Owner NDID
    
    // Status: "idle", "armed", "ringing", "unlocked"
    val status: String = "idle",
    val lastRungTime: Long = 0L,
    val nextTriggerMillis: Long = 0L
)
