package com.mmushtaq.smartreceiptscanner.core.export

import com.mmushtaq.smartreceiptscanner.core.data.db.MerchantPatternEntity
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import org.junit.Assert.*
import org.junit.Test

class JsonBackupTest {

    private fun sampleReceipt(id: String = "r1") = ReceiptEntity(
        id = id,
        imageUri = "content://images/$id",
        rawText = "AL-FAISAL MART\n12/08/2025\nGrand Total: Rs 400.00",
        createdAt = 1_723_100_000_000L,
        dateEpochMs = 1_723_000_000_000L,
        merchant = "AL-FAISAL MART",
        totalMinor = 40000L,
        currency = "PKR",
        category = "groceries"
    )

    private fun sparseReceipt(id: String = "r2") = ReceiptEntity(
        id = id,
        imageUri = "content://images/$id",
        rawText = "unparsed text",
        createdAt = 1_723_100_000_000L,
        dateEpochMs = null,
        merchant = null,
        totalMinor = null,
        currency = null,
        category = null
    )

    private fun samplePattern() = MerchantPatternEntity(
        merchantKey = "al-faisal mart",
        category = "groceries",
        currency = "PKR",
        updatedAt = 1_723_100_000_000L
    )

    @Test
    fun round_trip_preserves_fully_populated_receipt() {
        val original = sampleReceipt()
        val json = JsonBackup.toJsonString(listOf(original), emptyList())
        val restored = JsonBackup.parse(json)

        assertEquals(1, restored.receipts.size)
        assertEquals(original, restored.receipts.first())
    }

    @Test
    fun round_trip_preserves_receipt_with_null_fields() {
        val original = sparseReceipt()
        val json = JsonBackup.toJsonString(listOf(original), emptyList())
        val restored = JsonBackup.parse(json)

        assertEquals(1, restored.receipts.size)
        assertEquals(original, restored.receipts.first())
    }

    @Test
    fun round_trip_preserves_merchant_patterns() {
        val original = samplePattern()
        val json = JsonBackup.toJsonString(emptyList(), listOf(original))
        val restored = JsonBackup.parse(json)

        assertEquals(1, restored.merchantPatterns.size)
        assertEquals(original, restored.merchantPatterns.first())
    }

    @Test
    fun round_trip_preserves_multiple_receipts_in_order() {
        val originals = listOf(sampleReceipt("r1"), sparseReceipt("r2"), sampleReceipt("r3"))
        val json = JsonBackup.toJsonString(originals, emptyList())
        val restored = JsonBackup.parse(json)

        assertEquals(originals, restored.receipts)
    }

    @Test
    fun parse_handles_empty_backup() {
        val json = JsonBackup.toJsonString(emptyList(), emptyList())
        val restored = JsonBackup.parse(json)

        assertTrue(restored.receipts.isEmpty())
        assertTrue(restored.merchantPatterns.isEmpty())
    }
}
