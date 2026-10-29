package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.example.data.database.NDAlarmDatabase

class NDAlarmApp : Application() {

    val database: NDAlarmDatabase by lazy {
        NDAlarmDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High priority Ringing Channel for full screen alarms
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_RINGING,
                "ND Alarm Ringing Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for active ND Alarm alerts with full-screen anti-cheat prompt"
                setSound(null, null) // Sound is handled by foreground player
                enableVibration(true)
                lockscreenVisibility = NotificationManager.IMPORTANCE_HIGH
            }

            // Normal service / reminder channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ALARM_SCHEDULE,
                "ND Alarm Upcoming Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Upcoming alarm schedules and sync status"
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ALARM_RINGING = "nd_alarm_ringing_channel"
        const val CHANNEL_ALARM_SCHEDULE = "nd_alarm_schedule_channel"
    }
}
