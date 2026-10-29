package com.example.data.repository

import com.example.data.dao.AlarmDao
import com.example.data.dao.RingingHistoryDao
import com.example.data.dao.TrustedNdidDao
import com.example.data.model.AlarmEntity
import com.example.data.model.RingingHistoryEntity
import com.example.data.model.TrustedNdidEntity
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val trustedNdidDao: TrustedNdidDao,
    private val ringingHistoryDao: RingingHistoryDao
) {
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()
    val enabledAlarms: Flow<List<AlarmEntity>> = alarmDao.getEnabledAlarms()
    val allTrustedNdids: Flow<List<TrustedNdidEntity>> = trustedNdidDao.getAllTrustedNdids()
    val ringingHistory: Flow<List<RingingHistoryEntity>> = ringingHistoryDao.getAllHistory()

    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    suspend fun getAlarmByClockId(clockId: String): AlarmEntity? = alarmDao.getAlarmByClockId(clockId)

    suspend fun getEnabledAlarmsList(): List<AlarmEntity> = alarmDao.getEnabledAlarmsList()

    suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

    suspend fun updateStatus(id: Long, status: String) = alarmDao.updateStatus(id, status)

    suspend fun updateStatusByClockId(clockId: String, status: String) = alarmDao.updateStatusByClockId(clockId, status)

    suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

    suspend fun deleteAlarmById(id: Long) = alarmDao.deleteAlarmById(id)

    suspend fun insertTrustedNdid(trusted: TrustedNdidEntity): Long = trustedNdidDao.insertTrustedNdid(trusted)

    suspend fun deleteTrustedNdid(trusted: TrustedNdidEntity) = trustedNdidDao.deleteTrustedNdid(trusted)

    suspend fun isTrustedNdid(ndid: String): Boolean = trustedNdidDao.getByNdid(ndid) != null

    suspend fun logRingingHistory(history: RingingHistoryEntity): Long = ringingHistoryDao.insertHistory(history)

    suspend fun clearHistory() = ringingHistoryDao.clearHistory()
}
