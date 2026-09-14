package com.mmushtaq.smartreceiptscanner.core.parser

import com.mmushtaq.smartreceiptscanner.core.data.db.MerchantPatternDao
import com.mmushtaq.smartreceiptscanner.core.data.db.MerchantPatternEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** In-memory fake — MerchantPatternDao is a Room DAO, not unit-testable directly. */
private class FakeMerchantPatternDao : MerchantPatternDao {
    val table = mutableMapOf<String, MerchantPatternEntity>()
    override suspend fun get(merchantKey: String): MerchantPatternEntity? = table[merchantKey]
    override suspend fun getAllOnce(): List<MerchantPatternEntity> = table.values.toList()
    override suspend fun upsert(entity: MerchantPatternEntity) {
        table[entity.merchantKey] = entity
    }
    override suspend fun insertAll(entities: List<MerchantPatternEntity>) {
        entities.forEach { table[it.merchantKey] = it }
    }
}

class MerchantPatternStoreTest {

    @Test
    fun records_and_recalls_a_correction() = runBlocking {
        val dao = FakeMerchantPatternDao()
        val store = MerchantPatternStore(dao)

        store.recordCorrection("AL-FAISAL MART", category = "groceries", currency = "PKR")
        val found = store.lookup("AL-FAISAL MART")

        assertEquals("groceries", found?.category)
        assertEquals("PKR", found?.currency)
    }

    @Test
    fun lookup_is_case_and_whitespace_insensitive() = runBlocking {
        val dao = FakeMerchantPatternDao()
        val store = MerchantPatternStore(dao)

        store.recordCorrection("  AL-Faisal Mart  ", category = "groceries", currency = "PKR")
        val found = store.lookup("al-faisal mart")

        assertEquals("groceries", found?.category)
    }

    @Test
    fun lookup_returns_null_for_unknown_merchant() = runBlocking {
        val store = MerchantPatternStore(FakeMerchantPatternDao())
        assertNull(store.lookup("Never Seen Before"))
    }

    @Test
    fun recordCorrection_ignores_blank_or_null_merchant() = runBlocking {
        val dao = FakeMerchantPatternDao()
        val store = MerchantPatternStore(dao)

        store.recordCorrection(null, category = "groceries", currency = "PKR")
        store.recordCorrection("   ", category = "groceries", currency = "PKR")

        assertTrue(dao.table.isEmpty())
    }

    @Test
    fun recordCorrection_is_last_write_wins() = runBlocking {
        val dao = FakeMerchantPatternDao()
        val store = MerchantPatternStore(dao)

        store.recordCorrection("Corner Cafe", category = "dining", currency = "PKR")
        store.recordCorrection("Corner Cafe", category = "shopping", currency = "USD")

        val found = store.lookup("Corner Cafe")
        assertEquals("shopping", found?.category)
        assertEquals("USD", found?.currency)
    }
}
