package com.example.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.NDAlarmApp
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingtoneService : Service() {

    private var soundEngine: SoundEngine? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        soundEngine = SoundEngine(this)

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NDAlarm:RingtoneServiceWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // Max 10 minutes wake lock

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

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

        // Update database alarm status to "ringing"
        serviceScope.launch {
            val app = application as? NDAlarmApp
            app?.database?.alarmDao()?.updateStatus(alarmId, "ringing")
        }

        // Build full screen intent for ringing activity
        val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            ringingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, NDAlarmApp.CHANNEL_ALARM_RINGING)
            .setSmallIcon(R.drawable.nd_alarm_icon_1786901083667)
            .setContentTitle("ND Alarm: $label")
            .setContentText("Đang reo chuông! Nhập mật khẩu 50 ký tự hoặc tắt qua Web Portal.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Start audio synthesis
        soundEngine?.startPlaying(ringtone, volume)

        // Start vibration
        if (vibrate && vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        soundEngine?.stopPlaying()
        vibrator?.cancel()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 2001

        fun stopRinging(context: Context) {
            val intent = Intent(context, AlarmRingtoneService::class.java)
            context.stopService(intent)
        }
    }
}
