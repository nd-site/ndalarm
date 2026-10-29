package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.NDAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Boot or Time Changed received: ${intent.action}")
        val app = context.applicationContext as? NDAlarmApp ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val enabledAlarms = app.database.alarmDao().getEnabledAlarmsList()
            for (alarm in enabledAlarms) {
                val nextTrigger = AlarmScheduler.scheduleAlarm(context, alarm)
                app.database.alarmDao().updateAlarm(alarm.copy(nextTriggerMillis = nextTrigger, status = "armed"))
            }
            Log.d("BootReceiver", "Rescheduled ${enabledAlarms.size} alarms successfully.")
        }
    }
}
