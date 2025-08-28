package com.mmushtaq.smartreceiptscanner.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val imageUri: String,
    val rawText: String,
    val createdAt: Long,          // when saved
    val dateEpochMs: Long?,       // parsed later (for now null)
    val merchant: String?,        // parsed later
    val totalMinor: Long?,        // parsed later
    val currency: String?         // parsed later
)
