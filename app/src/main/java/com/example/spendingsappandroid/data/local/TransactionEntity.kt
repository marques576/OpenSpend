package com.example.spendingsappandroid.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["hash"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val currency: String,
    val merchant: String,
    val timestamp: Long,
    val sourceApp: String,
    val hash: String,
    val notificationTitle: String = "",
    val notificationText: String = ""
)
