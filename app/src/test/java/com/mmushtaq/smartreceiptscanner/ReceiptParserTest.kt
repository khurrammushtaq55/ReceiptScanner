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
}
