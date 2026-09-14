package com.mmushtaq.smartreceiptscanner.core.data

import org.junit.Assert.*
import org.junit.Test

class CategoriesTest {

    @Test
    fun guesses_groceries_for_mart() {
        assertEquals(Categories.Groceries, Categories.guessFromMerchant("AL-FAISAL MART"))
    }

    @Test
    fun guesses_fuel_for_petrol_station() {
        assertEquals(Categories.Fuel, Categories.guessFromMerchant("Shell Petrol Station"))
    }

    @Test
    fun guesses_dining_for_cafe() {
        assertEquals(Categories.Dining, Categories.guessFromMerchant("Corner Cafe"))
    }

    @Test
    fun returns_null_for_unknown_merchant() {
        assertNull(Categories.guessFromMerchant("Generic Shop Name"))
    }

    @Test
    fun returns_null_for_blank_or_null_merchant() {
        assertNull(Categories.guessFromMerchant(null))
        assertNull(Categories.guessFromMerchant("  "))
    }

    @Test
    fun byId_falls_back_to_other_for_unknown_id() {
        assertEquals(Categories.Other, Categories.byId("not-a-real-id"))
        assertEquals(Categories.Other, Categories.byId(null))
    }
}
