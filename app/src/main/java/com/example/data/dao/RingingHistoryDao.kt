package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RingingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RingingHistoryDao {
    @Query("SELECT * FROM ringing_history ORDER BY triggeredTime DESC LIMIT 100")
    fun getAllHistory(): Flow<List<RingingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: RingingHistoryEntity): Long

    @Query("DELETE FROM ringing_history")
    suspend fun clearHistory()
}
