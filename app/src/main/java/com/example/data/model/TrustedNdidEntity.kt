package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trusted_ndids")
data class TrustedNdidEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ndid: String,
    val label: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val canBypassPin: Boolean = true
)
