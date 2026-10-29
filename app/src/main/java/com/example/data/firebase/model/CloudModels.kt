package com.example.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val clockId: String = "",
    val ndid: String = "",
    @field:ServerTimestamp @get:PropertyName("createdAt") val createdAt: Timestamp? = null,
    @field:ServerTimestamp @get:PropertyName("updatedAt") val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudAlarm(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val hour: Int = 0,
    val minute: Int = 0,
    @get:PropertyName("isEnabled") val isEnabled: Boolean = true,
    @get:PropertyName("isLunarCalendar") val isLunarCalendar: Boolean = false,
    val repeatRule: String = "DAILY",
    val lunarDay: Int = 1,
    val lunarMonth: Int = 0,
    val ringtoneType: String = "gentle_breeze",
    val vibrate: Boolean = true,
    val antiCheatMode: String = "STANDARD",
    val clockId: String = "",
    val ndid: String = "",
    val status: String = "armed",
    @field:ServerTimestamp @get:PropertyName("createdAt") val createdAt: Timestamp? = null,
    @field:ServerTimestamp @get:PropertyName("updatedAt") val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudTrustedNdid(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ndid: String = "",
    val label: String = "",
    @field:ServerTimestamp @get:PropertyName("createdAt") val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudRingingHistory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val alarmId: Long = 0L,
    val alarmLabel: String = "",
    val triggeredTime: Long = 0L,
    val unlockedTime: Long = 0L,
    val unlockMethod: String = "",
    val ndid: String = "",
    @field:ServerTimestamp @get:PropertyName("createdAt") val createdAt: Timestamp? = null
)
