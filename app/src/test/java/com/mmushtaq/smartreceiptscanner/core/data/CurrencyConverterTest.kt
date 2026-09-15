package com.mmushtaq.smartreceiptscanner.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun converts_whole_amount_correctly() {
        // 100.00 USD * 278.50 = 27850.00 PKR
        val result = CurrencyConverter.convertToBase(10000L, 278.5)
        assertEquals(2785000L, result)
    }

    @Test
    fun rounds_to_nearest_minor_unit() {
        // 1.00 USD * 278.555 = 278.555 -> rounds to 278.56 (27856 minor)
        val result = CurrencyConverter.convertToBase(100L, 278.555)
        assertEquals(27856L, result)
    }

    @Test
    fun rounds_down_when_below_half() {
        // 1.00 USD * 278.554 = 278.554 -> rounds to 278.55 (27855 minor)
        val result = CurrencyConverter.convertToBase(100L, 278.554)
        assertEquals(27855L, result)
    }

    @Test
    fun zero_amount_converts_to_zero() {
        assertEquals(0L, CurrencyConverter.convertToBase(0L, 278.5))
    }

    @Test
    fun rate_of_one_is_identity_in_minor_units() {
        assertEquals(12345L, CurrencyConverter.convertToBase(12345L, 1.0))
    }
}
