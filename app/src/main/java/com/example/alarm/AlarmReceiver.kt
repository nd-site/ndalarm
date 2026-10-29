package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm fired! Action: ${intent.action}")

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "NDAlarm:AlarmReceiverWakeLock"
        )
        wakeLock?.acquire(30000L) // 30 seconds

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, 0L)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Báo thức ND"
        val ringtone = intent.getStringExtra(AlarmScheduler.EXTRA_RINGTONE) ?: "gentle_breeze"
        val vibrate = intent.getBooleanExtra(AlarmScheduler.EXTRA_VIBRATE, true)
        val volume = intent.getFloatExtra(AlarmScheduler.EXTRA_VOLUME, 1.0f)
        val antiCheat = intent.getStringExtra(AlarmScheduler.EXTRA_ANTI_CHEAT) ?: "TOKEN_50"
        val token = intent.getStringExtra(AlarmScheduler.EXTRA_TOKEN_50) ?: ""
        val pin = intent.getStringExtra(AlarmScheduler.EXTRA_PIN_8) ?: ""
        val clockId = intent.getStringExtra(AlarmScheduler.EXTRA_CLOCK_ID) ?: "ND-CL01"
        val ndid = intent.getStringExtra(AlarmScheduler.EXTRA_NDID) ?: "ND-USER"

        // 1. Start Ringing Foreground Service
        val serviceIntent = Intent(context, AlarmRingtoneService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmScheduler.EXTRA_RINGTONE, ringtone)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmScheduler.EXTRA_VOLUME, volume)
            putExtra(AlarmScheduler.EXTRA_ANTI_CHEAT, antiCheat)
            putExtra(AlarmScheduler.EXTRA_TOKEN_50, token)
            putExtra(AlarmScheduler.EXTRA_PIN_8, pin)
            putExtra(AlarmScheduler.EXTRA_CLOCK_ID, clockId)
            putExtra(AlarmScheduler.EXTRA_NDID, ndid)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 2. Launch Full Screen Ringing Activity with anti-cheat lock
        val ringingActivityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmScheduler.EXTRA_RINGTONE, ringtone)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmScheduler.EXTRA_ANTI_CHEAT, antiCheat)
            putExtra(AlarmScheduler.EXTRA_TOKEN_50, token)
            putExtra(AlarmScheduler.EXTRA_PIN_8, pin)
            putExtra(AlarmScheduler.EXTRA_CLOCK_ID, clockId)
            putExtra(AlarmScheduler.EXTRA_NDID, ndid)
        }
        context.startActivity(ringingActivityIntent)

        wakeLock?.release()
    }
}
