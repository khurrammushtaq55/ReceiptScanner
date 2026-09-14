package com.mmushtaq.smartreceiptscanner.core.parser

import com.mmushtaq.smartreceiptscanner.core.data.db.MerchantPatternDao
import com.mmushtaq.smartreceiptscanner.core.data.db.MerchantPatternEntity

/**
 * Learns from user corrections on a per-merchant basis. Intentionally simple (last-write-wins):
 * whatever category/currency the user last picked for a merchant is offered next time as a
 * starting point in Review — never applied silently to already-saved receipts.
 */
class MerchantPatternStore(private val dao: MerchantPatternDao) {

    suspend fun lookup(merchant: String?): MerchantPatternEntity? {
        val key = normalize(merchant) ?: return null
        return dao.get(key)
    }

    /** One-shot read of all remembered corrections — for backup export. */
    suspend fun getAllOnce(): List<MerchantPatternEntity> = dao.getAllOnce()

    /** Bulk restore from a JSON backup. */
    suspend fun importAll(entities: List<MerchantPatternEntity>) = dao.insertAll(entities)

    suspend fun recordCorrection(merchant: String?, category: String?, currency: String?) {
        val key = normalize(merchant) ?: return
        if (category == null && currency == null) return
        dao.upsert(
            MerchantPatternEntity(
                merchantKey = key,
                category = category,
                currency = currency,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun normalize(merchant: String?): String? =
        merchant?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
}
