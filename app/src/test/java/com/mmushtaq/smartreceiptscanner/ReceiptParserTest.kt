package com.mmushtaq.smartreceiptscanner.core.parser

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ReceiptParserTest {

    @Test
    fun parse_basic_pk_receipt() {
        val ocr = """
            AL-FAISAL MART
            12/08/2025
            Item A ........ 350.00
            GST ........... 50.00
            Grand Total: Rs 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertEquals("PKR", p.currency)
        assertEquals(40000L, p.totalMinor) // 400.00
        assertNotNull(p.dateEpochMs)
        assertEquals("AL-FAISAL MART", p.merchant)
        assertEquals(5000L, p.taxMinor) // 50.00
    }

    @Test
    fun parse_textual_date_usd() {
        val ocr = """
            CAFE SOMETHING
            Aug 10, 2025
            Total $12.34
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "USD")
        assertEquals("USD", p.currency)
        assertEquals(1234L, p.totalMinor)
        assertEquals("CAFE SOMETHING", p.merchant)
    }

    @Test
    fun parse_suggests_category_from_merchant() {
        val ocr = """
            AL-FAISAL MART
            12/08/2025
            Grand Total: Rs 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertEquals("groceries", p.suggestedCategory)
    }

    @Test
    fun parse_leaves_category_null_when_merchant_unknown() {
        val ocr = """
            GENERIC SHOP NAME
            12/08/2025
            Grand Total: Rs 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertEquals("GENERIC SHOP NAME", p.merchant)
        assertNull(p.suggestedCategory)
    }

    @Test
    fun merchant_name_containing_business_type_word_is_not_rejected() {
        // Regression: "mart"/"store"/"pharmacy"/"restaurant" are common *inside* real merchant
        // names and must not be treated as noise (previously caused merchant to come back null).
        val samples = listOf(
            "AL-FAISAL MART" to "12/08/2025\nGrand Total: Rs 400.00",
            "CITY PHARMACY" to "01/02/2024\nTotal 250.00",
            "AL BAIK RESTAURANT" to "03/04/2024\nTotal 900.00"
        )
        samples.forEach { (merchantLine, rest) ->
            val ocr = "$merchantLine\n$rest"
            val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
            assertEquals(merchantLine, p.merchant)
        }
    }

    @Test
    fun total_confidence_high_when_hint_line_matched() {
        val ocr = """
            AL-FAISAL MART
            12/08/2025
            Grand Total: Rs 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertEquals(0.9f, p.confidence[ReceiptParser.Field.TOTAL])
    }

    @Test
    fun total_confidence_low_when_no_hint_line_present() {
        val ocr = """
            GENERIC SHOP NAME
            12/08/2025
            Item A 100.00
            Item B 200.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertEquals(20000L, p.totalMinor) // fell back to the largest amount near the bottom
        assertEquals(0.4f, p.confidence[ReceiptParser.Field.TOTAL])
    }

    @Test
    fun currency_confidence_low_when_no_explicit_symbol_found() {
        val ocr = """
            GENERIC SHOP NAME
            12/08/2025
            Total 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertEquals("PKR", p.currency) // silently defaulted, not detected
        assertEquals(0.3f, p.confidence[ReceiptParser.Field.CURRENCY])
    }

    @Test
    fun currency_confidence_high_when_symbol_detected() {
        val ocr = """
            AL-FAISAL MART
            12/08/2025
            Grand Total: Rs 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "USD")
        assertEquals("PKR", p.currency) // detected from "Rs", overriding the passed-in default
        assertEquals(0.9f, p.confidence[ReceiptParser.Field.CURRENCY])
    }

    @Test
    fun merchant_and_date_confidence_zero_when_not_found() {
        val ocr = """
            123 456
            Total 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertNull(p.merchant)
        assertNull(p.dateEpochMs)
        assertEquals(0f, p.confidence[ReceiptParser.Field.MERCHANT])
        assertEquals(0f, p.confidence[ReceiptParser.Field.DATE])
    }

    @Test
    fun tax_confidence_zero_when_no_tax_line_present() {
        val ocr = """
            AL-FAISAL MART
            12/08/2025
            Total 400.00
        """.trimIndent()

        val p = ReceiptParser.parse(ocr, defaultCurrency = "PKR")
        assertNull(p.taxMinor)
        assertEquals(0f, p.confidence[ReceiptParser.Field.TAX])
    }
}
