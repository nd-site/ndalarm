package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NDAlarmApp
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmScheduler
import com.example.data.firebase.FirestoreAlarmRepository
import com.example.data.firebase.model.CloudAlarm
import com.example.data.firebase.model.CloudRingingHistory
import com.example.data.firebase.model.CloudTrustedNdid
import com.example.data.firebase.model.CloudUserProfile
import com.example.data.model.AlarmEntity
import com.example.data.model.RingingHistoryEntity
import com.example.data.model.TrustedNdidEntity
import com.example.data.repository.AlarmRepository
import com.example.util.LunarCalendarHelper
import com.example.util.SecurityHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "AlarmViewModel"

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val firestoreRepository: FirestoreAlarmRepository
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

    private val _currentUser = MutableStateFlow<FirebaseUser?>(Firebase.auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    private val _cloudSyncStatus = MutableStateFlow("Đã kết nối")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
        if (auth.currentUser != null) {
            syncWithCloud()
        }
    }

    init {
        val app = application as NDAlarmApp
        repository = AlarmRepository(
            app.database.alarmDao(),
            app.database.trustedNdidDao(),
            app.database.ringingHistoryDao()
        )
        firestoreRepository = FirestoreAlarmRepository(application)

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

        Firebase.auth.addAuthStateListener(authListener)

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

    override fun onCleared() {
        super.onCleared()
        Firebase.auth.removeAuthStateListener(authListener)
    }

    fun syncWithCloud() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _cloudSyncStatus.value = "Đang đồng bộ..."
            try {
                // Save user profile
                firestoreRepository.saveUserProfile(
                    CloudUserProfile(
                        userId = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "ND User",
                        clockId = _clockId.value,
                        ndid = _ndid.value
                    )
                )

                // Sync current alarms to Firestore
                val localAlarms = repository.getEnabledAlarmsList()
                for (alarm in localAlarms) {
                    firestoreRepository.saveAlarm(
                        CloudAlarm(
                            id = alarm.id.toString(),
                            userId = user.uid,
                            label = alarm.label,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            isEnabled = alarm.isEnabled,
                            isLunarCalendar = alarm.isLunarCalendar,
                            repeatRule = alarm.repeatRule,
                            lunarDay = alarm.lunarDay,
                            lunarMonth = alarm.lunarMonth,
                            ringtoneType = alarm.ringtoneType,
                            vibrate = alarm.vibrate,
                            antiCheatMode = alarm.antiCheatMode,
                            clockId = alarm.clockId,
                            ndid = alarm.ndid,
                            status = alarm.status
                        )
                    )
                }

                _cloudSyncStatus.value = "Đã đồng bộ Cloud (ndlabs-0)"
            } catch (e: Exception) {
                Log.e(TAG, "Đồng bộ Firestore thất bại", e)
                _cloudSyncStatus.value = "Lỗi đồng bộ: ${e.localizedMessage ?: "Thử lại sau"}"
            } finally {
                _isCloudSyncing.value = false
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

            // Sync to Firestore if authenticated
            val user = _currentUser.value
            if (user != null) {
                try {
                    firestoreRepository.saveAlarm(
                        CloudAlarm(
                            id = id.toString(),
                            userId = user.uid,
                            label = alarm.label,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            isEnabled = alarm.isEnabled,
                            isLunarCalendar = alarm.isLunarCalendar,
                            repeatRule = alarm.repeatRule,
                            lunarDay = alarm.lunarDay,
                            lunarMonth = alarm.lunarMonth,
                            ringtoneType = alarm.ringtoneType,
                            vibrate = alarm.vibrate,
                            antiCheatMode = alarm.antiCheatMode,
                            clockId = _clockId.value,
                            ndid = _ndid.value,
                            status = "armed"
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Không thể lưu alarm lên Firestore", e)
                }
            }

            closeAddAlarmDialog()
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            val nextMillis = if (updated.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
                0L
            }
            val status = if (updated.isEnabled) "armed" else "idle"
            val finalAlarm = updated.copy(nextTriggerMillis = nextMillis, status = status)
            repository.updateAlarm(finalAlarm)

            // Update in Firestore if authenticated
            val user = _currentUser.value
            if (user != null) {
                try {
                    firestoreRepository.saveAlarm(
                        CloudAlarm(
                            id = alarm.id.toString(),
                            userId = user.uid,
                            label = finalAlarm.label,
                            hour = finalAlarm.hour,
                            minute = finalAlarm.minute,
                            isEnabled = finalAlarm.isEnabled,
                            isLunarCalendar = finalAlarm.isLunarCalendar,
                            repeatRule = finalAlarm.repeatRule,
                            lunarDay = finalAlarm.lunarDay,
                            lunarMonth = finalAlarm.lunarMonth,
                            ringtoneType = finalAlarm.ringtoneType,
                            vibrate = finalAlarm.vibrate,
                            antiCheatMode = finalAlarm.antiCheatMode,
                            clockId = finalAlarm.clockId,
                            ndid = finalAlarm.ndid,
                            status = status
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi cập nhật alarm trên Firestore", e)
                }
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
            repository.deleteAlarm(alarm)

            // Delete in Firestore if authenticated
            val user = _currentUser.value
            if (user != null) {
                try {
                    firestoreRepository.deleteAlarm(alarm.id.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi xóa alarm trên Firestore", e)
                }
            }
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
            val entity = TrustedNdidEntity(
                ndid = ndid.trim().uppercase(),
                label = label.ifBlank { "Thiết bị tin cậy" }
            )
            val localId = repository.insertTrustedNdid(entity)

            val user = _currentUser.value
            if (user != null) {
                try {
                    firestoreRepository.saveTrustedNdid(
                        CloudTrustedNdid(
                            id = localId.toString(),
                            userId = user.uid,
                            ndid = entity.ndid,
                            label = entity.label
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi thêm trusted NDID trên Firestore", e)
                }
            }
        }
    }

    fun removeTrustedNdid(trusted: TrustedNdidEntity) {
        viewModelScope.launch {
            repository.deleteTrustedNdid(trusted)

            val user = _currentUser.value
            if (user != null) {
                try {
                    firestoreRepository.deleteTrustedNdid(trusted.id.toString())
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi xóa trusted NDID trên Firestore", e)
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun recordHistory(historyItem: RingingHistoryEntity) {
        viewModelScope.launch {
            val localId = repository.logRingingHistory(historyItem)
            val user = _currentUser.value
            if (user != null) {
                try {
                    firestoreRepository.recordHistory(
                        CloudRingingHistory(
                            id = localId.toString(),
                            userId = user.uid,
                            alarmId = historyItem.alarmId,
                            alarmLabel = historyItem.alarmLabel,
                            triggeredTime = historyItem.triggeredTime,
                            unlockedTime = historyItem.unlockedTime,
                            unlockMethod = historyItem.unlockMethod,
                            ndid = historyItem.unlockedByNdid
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi lưu history trên Firestore", e)
                }
            }
        }
    }

    fun regenerateClockId() {
        _clockId.value = SecurityHelper.generateClockId()
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                try {
                    firestoreRepository.saveUserProfile(
                        CloudUserProfile(
                            userId = user.uid,
                            email = user.email ?: "",
                            displayName = user.displayName ?: "ND User",
                            clockId = _clockId.value,
                            ndid = _ndid.value
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi cập nhật clockId trên Firestore", e)
                }
            }
        }
    }
}
