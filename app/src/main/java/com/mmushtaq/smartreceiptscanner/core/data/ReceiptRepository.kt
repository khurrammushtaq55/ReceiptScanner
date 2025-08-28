package com.mmushtaq.smartreceiptscanner.core.data

import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptDao
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ReceiptRepository(private val dao: ReceiptDao) {

    fun observeReceipts(): Flow<List<ReceiptEntity>> = dao.observeAll()

    suspend fun saveBasic(imageUri: String, rawText: String, merchant: String) : String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = ReceiptEntity(
            id = id,
            imageUri = imageUri,
            rawText = rawText,
            createdAt = now,
            dateEpochMs = null,
            merchant = merchant,
            totalMinor = null,
            currency = null
        )
        dao.insert(entity)
        return id
    }
    suspend fun updateParsed(
        id: String,
        merchant: String?,
        dateEpochMs: Long?,
        currency: String?,
        totalMinor: Long?,
        taxMinor: Long? // kept for later if you store line items
    ) {
        val existing = dao.get(id) ?: return
        dao.insert(existing.copy(
            merchant = merchant ?: existing.merchant,
            dateEpochMs = dateEpochMs ?: existing.dateEpochMs,
            currency = currency ?: existing.currency,
            totalMinor = totalMinor ?: existing.totalMinor
        ))
    }

    suspend fun get(id: String) = dao.get(id)
    suspend fun delete(entity: ReceiptEntity) = dao.delete(entity)
}
