package com.mmushtaq.smartreceiptscanner.core.export

import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvExporterTest {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US) // matches CsvExporter's own formatter

    private fun receipt(
        merchant: String? = "AL-FAISAL MART",
        totalMinor: Long? = 40000L,
        currency: String? = "PKR",
        category: String? = "groceries",
        dateEpochMs: Long? = 1_723_000_000_000L,
        createdAt: Long = 1_723_100_000_000L
    ) = ReceiptEntity(
        id = "r1",
        imageUri = "content://fake",
        rawText = "raw",
        createdAt = createdAt,
        dateEpochMs = dateEpochMs,
        merchant = merchant,
        totalMinor = totalMinor,
        currency = currency,
        category = category
    )

    @Test
    fun header_row_is_present() {
        val csv = CsvExporter.toCsvString(emptyList())
        assertEquals("Merchant,Date,Total,Currency,Category", csv.trim())
    }

    @Test
    fun basic_row_formats_correctly() {
        val r = receipt()
        val csv = CsvExporter.toCsvString(listOf(r))
        val lines = csv.trim().lines()
        assertEquals(2, lines.size)
        val expectedDate = dateFmt.format(Date(r.dateEpochMs!!))
        assertEquals("AL-FAISAL MART,$expectedDate,400.00,PKR,Groceries", lines[1])
    }

    @Test
    fun missing_fields_render_as_empty_cells() {
        val r = receipt(merchant = null, totalMinor = null, currency = null, category = null)
        val csv = CsvExporter.toCsvString(listOf(r))
        val dataLine = csv.trim().lines()[1]
        assertTrue(dataLine.startsWith(","))
        assertTrue(dataLine.endsWith(",,,"))
    }

    @Test
    fun falls_back_to_createdAt_when_dateEpochMs_is_null() {
        val r = receipt(dateEpochMs = null, createdAt = 1_723_100_000_000L)
        val csv = CsvExporter.toCsvString(listOf(r))
        val dataLine = csv.trim().lines()[1]
        val expectedDate = dateFmt.format(Date(r.createdAt))
        assertTrue(dataLine.contains(expectedDate))
    }

    @Test
    fun merchant_with_comma_is_quoted_and_escaped() {
        val r = receipt(merchant = "Al Faisal, Mart \"Downtown\"")
        val csv = CsvExporter.toCsvString(listOf(r))
        val dataLine = csv.trim().lines()[1]
        assertTrue(dataLine.startsWith("\"Al Faisal, Mart \"\"Downtown\"\"\","))
    }

    @Test
    fun csvEscape_leaves_plain_values_untouched() {
        assertEquals("Groceries", CsvExporter.csvEscape("Groceries"))
    }

    @Test
    fun csvEscape_quotes_values_with_special_characters() {
        assertEquals("\"a,b\"", CsvExporter.csvEscape("a,b"))
        assertEquals("\"a\"\"b\"", CsvExporter.csvEscape("a\"b"))
        assertEquals("\"a\nb\"", CsvExporter.csvEscape("a\nb"))
    }

    @Test
    fun no_converted_column_when_baseCurrency_not_provided() {
        val csv = CsvExporter.toCsvString(listOf(receipt()))
        assertEquals("Merchant,Date,Total,Currency,Category", csv.trim().lines()[0])
    }

    @Test
    fun converted_column_added_when_baseCurrency_provided() {
        val csv = CsvExporter.toCsvString(listOf(receipt()), baseCurrency = "PKR", rates = emptyMap())
        assertEquals("Merchant,Date,Total,Currency,Category,Converted Total (PKR)", csv.trim().lines()[0])
    }

    @Test
    fun convertedTotalText_is_identity_for_base_currency() {
        val text = CsvExporter.convertedTotalText(40000L, "PKR", "PKR", emptyMap())
        assertEquals("400.00", text)
    }

    @Test
    fun convertedTotalText_converts_using_known_rate() {
        // 50.00 USD * 278.5 = 13925.00
        val text = CsvExporter.convertedTotalText(5000L, "USD", "PKR", mapOf("USD" to 278.5))
        assertEquals("13925.00", text)
    }

    @Test
    fun convertedTotalText_is_blank_when_rate_unknown() {
        val text = CsvExporter.convertedTotalText(5000L, "EUR", "PKR", mapOf("USD" to 278.5))
        assertEquals("", text)
    }

    @Test
    fun convertedTotalText_is_blank_for_null_amount_or_currency() {
        assertEquals("", CsvExporter.convertedTotalText(null, "USD", "PKR", emptyMap()))
        assertEquals("", CsvExporter.convertedTotalText(5000L, null, "PKR", emptyMap()))
    }
}
