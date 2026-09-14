package com.mmushtaq.smartreceiptscanner.core.data

import com.mmushtaq.smartreceiptscanner.core.data.db.CategoryTotal
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptDao
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class ReceiptRepository(private val dao: ReceiptDao) {

    fun observeReceipts(): Flow<List<ReceiptEntity>> = dao.observeAll()

    suspend fun saveBasic(
        imageUri: String,
        rawText: String,
        merchant: String,
        category: String? = null
    ) : String {
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
            currency = null,
            category = category
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
        taxMinor: Long?, // kept for later if you store line items
        category: String? = null
    ) {
        val existing = dao.get(id) ?: return
        dao.insert(existing.copy(
            merchant = merchant ?: existing.merchant,
            dateEpochMs = dateEpochMs ?: existing.dateEpochMs,
            currency = currency ?: existing.currency,
            totalMinor = totalMinor ?: existing.totalMinor,
            category = category ?: existing.category
        ))
    }

    suspend fun get(id: String) = dao.get(id)
    suspend fun delete(entity: ReceiptEntity) = dao.delete(entity)

    /** Spend-by-category for the current calendar month, keyed by createdAt (when the receipt was saved). */
    fun observeMonthlyCategoryTotals(): Flow<List<CategoryTotal>> {
        val (start, end) = currentMonthRange()
        return dao.observeCategoryTotals(start, end)
    }

    private fun currentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return start to end
    }
}
