package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TrustedNdidEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedNdidDao {
    @Query("SELECT * FROM trusted_ndids ORDER BY addedTimestamp DESC")
    fun getAllTrustedNdids(): Flow<List<TrustedNdidEntity>>

    @Query("SELECT * FROM trusted_ndids WHERE ndid = :ndid LIMIT 1")
    suspend fun getByNdid(ndid: String): TrustedNdidEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrustedNdid(trusted: TrustedNdidEntity): Long

    @Delete
    suspend fun deleteTrustedNdid(trusted: TrustedNdidEntity)

    @Query("DELETE FROM trusted_ndids WHERE id = :id")
    suspend fun deleteById(id: Long)
}
