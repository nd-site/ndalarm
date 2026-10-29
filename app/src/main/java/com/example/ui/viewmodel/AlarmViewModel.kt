package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NDAlarmApp
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmScheduler
import com.example.data.model.AlarmEntity
import com.example.data.model.RingingHistoryEntity
import com.example.data.model.TrustedNdidEntity
import com.example.data.repository.AlarmRepository
import com.example.util.LunarCalendarHelper
import com.example.util.SecurityHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    val alarms: StateFlow<List<AlarmEntity>>
    val trustedNdids: StateFlow<List<TrustedNdidEntity>>
    val history: StateFlow<List<RingingHistoryEntity>>

    private val _clockId = MutableStateFlow("ND-CL01")
    val clockId: StateFlow<String> = _clockId.asStateFlow()

    private val _ndid = MutableStateFlow("ND-MASTER")
    val ndid: StateFlow<String> = _ndid.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _editingAlarm = MutableStateFlow<AlarmEntity?>(null)
    val editingAlarm: StateFlow<AlarmEntity?> = _editingAlarm.asStateFlow()

    init {
        val app = application as NDAlarmApp
        repository = AlarmRepository(
            app.database.alarmDao(),
            app.database.trustedNdidDao(),
            app.database.ringingHistoryDao()
        )

        alarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        trustedNdids = repository.allTrustedNdids.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        history = repository.ringingHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed initial default alarms & trusted NDID if empty
        viewModelScope.launch {
            val list = repository.getEnabledAlarmsList()
            if (list.isEmpty()) {
                val defaultAlarm1 = AlarmEntity(
                    label = "Thức dậy sớm ND Labs",
                    hour = 7,
                    minute = 0,
                    isEnabled = true,
                    isLunarCalendar = false,
                    repeatRule = "DAILY",
                    ringtoneType = "gentle_breeze",
                    vibrate = true,
                    secretToken50 = SecurityHelper.generateSecretToken50(),
                    emergencyPin8 = SecurityHelper.generateEmergencyPin8(),
                    clockId = _clockId.value,
                    ndid = _ndid.value,
                    status = "armed"
                )
                val id1 = repository.insertAlarm(defaultAlarm1)
                val target1 = AlarmScheduler.scheduleAlarm(application, defaultAlarm1.copy(id = id1))
                repository.updateAlarm(defaultAlarm1.copy(id = id1, nextTriggerMillis = target1))

                // Default Lunar alarm for Mùng 1 & Rằm 15
                val lunarAlarm = AlarmEntity(
                    label = "Báo thức Mùng 1 & Rằm Âm Lịch",
                    hour = 6,
                    minute = 30,
                    isEnabled = true,
                    isLunarCalendar = true,
                    repeatRule = "LUNAR_1_15",
                    lunarDay = 1,
                    ringtoneType = "morning_ocean",
                    vibrate = true,
                    secretToken50 = SecurityHelper.generateSecretToken50(),
                    emergencyPin8 = SecurityHelper.generateEmergencyPin8(),
                    clockId = _clockId.value,
                    ndid = _ndid.value,
                    status = "armed"
                )
                val id2 = repository.insertAlarm(lunarAlarm)
                val target2 = AlarmScheduler.scheduleAlarm(application, lunarAlarm.copy(id = id2))
                repository.updateAlarm(lunarAlarm.copy(id = id2, nextTriggerMillis = target2))

                // Seed trusted NDID
                repository.insertTrustedNdid(
                    TrustedNdidEntity(
                        ndid = "ND-FAMILY-01",
                        label = "Người thân tin cậy (Bypass PIN)"
                    )
                )
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun openAddAlarmDialog(alarmToEdit: AlarmEntity? = null) {
        _editingAlarm.value = alarmToEdit
        _showAddDialog.value = true
    }

    fun closeAddAlarmDialog() {
        _showAddDialog.value = false
        _editingAlarm.value = null
    }

    fun saveAlarm(
        label: String,
        hour: Int,
        minute: Int,
        isLunar: Boolean,
        repeatRule: String,
        lunarDay: Int,
        lunarMonth: Int,
        ringtoneType: String,
        vibrate: Boolean,
        antiCheatMode: String
    ) {
        viewModelScope.launch {
            val existing = _editingAlarm.value
            val token = existing?.secretToken50?.takeIf { it.isNotEmpty() } ?: SecurityHelper.generateSecretToken50()
            val pin = existing?.emergencyPin8?.takeIf { it.isNotEmpty() } ?: SecurityHelper.generateEmergencyPin8()

            val alarm = AlarmEntity(
                id = existing?.id ?: 0L,
                label = label.ifBlank { "Báo thức ND" },
                hour = hour,
                minute = minute,
                isEnabled = true,
                isLunarCalendar = isLunar,
                repeatRule = repeatRule,
                lunarDay = lunarDay,
                lunarMonth = lunarMonth,
                ringtoneType = ringtoneType,
                vibrate = vibrate,
                antiCheatMode = antiCheatMode,
                secretToken50 = token,
                emergencyPin8 = pin,
                clockId = _clockId.value,
                ndid = _ndid.value,
                status = "armed"
            )

            val id = if (existing != null) {
                repository.updateAlarm(alarm)
                existing.id
            } else {
                repository.insertAlarm(alarm)
            }

            val nextMillis = AlarmScheduler.scheduleAlarm(getApplication(), alarm.copy(id = id))
            repository.updateAlarm(alarm.copy(id = id, nextTriggerMillis = nextMillis))
            closeAddAlarmDialog()
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            if (updated.isEnabled) {
                val nextMillis = AlarmScheduler.scheduleAlarm(getApplication(), updated)
                repository.updateAlarm(updated.copy(nextTriggerMillis = nextMillis, status = "armed"))
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
                repository.updateAlarm(updated.copy(nextTriggerMillis = 0L, status = "idle"))
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    fun triggerTestRinging(alarm: AlarmEntity) {
        val context = getApplication<NDAlarmApp>()
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_ALARM"
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmScheduler.EXTRA_RINGTONE, alarm.ringtoneType)
            putExtra(AlarmScheduler.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmScheduler.EXTRA_ANTI_CHEAT, alarm.antiCheatMode)
            putExtra(AlarmScheduler.EXTRA_TOKEN_50, alarm.secretToken50)
            putExtra(AlarmScheduler.EXTRA_PIN_8, alarm.emergencyPin8)
            putExtra(AlarmScheduler.EXTRA_CLOCK_ID, alarm.clockId)
            putExtra(AlarmScheduler.EXTRA_NDID, alarm.ndid)
        }
        context.sendBroadcast(intent)
    }

    fun addTrustedNdid(ndid: String, label: String) {
        if (ndid.isBlank()) return
        viewModelScope.launch {
            repository.insertTrustedNdid(
                TrustedNdidEntity(
                    ndid = ndid.trim().uppercase(),
                    label = label.ifBlank { "Thiết bị tin cậy" }
                )
            )
        }
    }

    fun removeTrustedNdid(trusted: TrustedNdidEntity) {
        viewModelScope.launch {
            repository.deleteTrustedNdid(trusted)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun regenerateClockId() {
        _clockId.value = SecurityHelper.generateClockId()
    }
}
