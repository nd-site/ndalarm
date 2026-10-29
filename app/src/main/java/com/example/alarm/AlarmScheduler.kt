package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.AlarmEntity
import com.example.util.LunarCalendarHelper
import java.util.Calendar

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
    const val EXTRA_ALARM_LABEL = "EXTRA_ALARM_LABEL"
    const val EXTRA_RINGTONE = "EXTRA_RINGTONE"
    const val EXTRA_VIBRATE = "EXTRA_VIBRATE"
    const val EXTRA_VOLUME = "EXTRA_VOLUME"
    const val EXTRA_ANTI_CHEAT = "EXTRA_ANTI_CHEAT"
    const val EXTRA_TOKEN_50 = "EXTRA_TOKEN_50"
    const val EXTRA_PIN_8 = "EXTRA_PIN_8"
    const val EXTRA_CLOCK_ID = "EXTRA_CLOCK_ID"
    const val EXTRA_NDID = "EXTRA_NDID"

    fun calculateTriggerMillis(alarm: AlarmEntity): Long {
        val now = System.currentTimeMillis()

        if (alarm.isLunarCalendar) {
            // Lunar calendar alarm
            val targetLunarDay = if (alarm.repeatRule == "LUNAR_1_15") {
                // If today is past 15, next is 1, else if past 1 next is 15
                val todayLunar = LunarCalendarHelper.getTodayLunarDate()
                if (todayLunar.day < 15) 15 else 1
            } else {
                alarm.lunarDay
            }
            return LunarCalendarHelper.calculateNextLunarTrigger(
                targetLunarDay = targetLunarDay,
                targetLunarMonth = alarm.lunarMonth,
                hour = alarm.hour,
                minute = alarm.minute
            )
        } else {
            // Solar calendar alarm
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, alarm.hour)
            cal.set(Calendar.MINUTE, alarm.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (cal.timeInMillis <= now) {
                // Alarm time has passed today, schedule for tomorrow
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }
    }

    fun scheduleAlarm(context: Context, alarm: AlarmEntity): Long {
        if (!alarm.isEnabled) return 0L

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0L
        val triggerMillis = calculateTriggerMillis(alarm)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_RINGTONE, alarm.ringtoneType)
            putExtra(EXTRA_VIBRATE, alarm.vibrate)
            putExtra(EXTRA_VOLUME, alarm.volume)
            putExtra(EXTRA_ANTI_CHEAT, alarm.antiCheatMode)
            putExtra(EXTRA_TOKEN_50, alarm.secretToken50)
            putExtra(EXTRA_PIN_8, alarm.emergencyPin8)
            putExtra(EXTRA_CLOCK_ID, alarm.clockId)
            putExtra(EXTRA_NDID, alarm.ndid)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Alarm ${alarm.id} (${alarm.label}) scheduled at $triggerMillis")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Exact alarm permission error", e)
        }

        return triggerMillis
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
