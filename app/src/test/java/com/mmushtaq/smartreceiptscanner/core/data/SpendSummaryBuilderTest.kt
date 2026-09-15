package com.mmushtaq.smartreceiptscanner.core.data

import com.mmushtaq.smartreceiptscanner.core.data.db.CategoryTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpendSummaryBuilderTest {

    @Test
    fun single_currency_matching_base_combines_directly() {
        val totals = listOf(
            CategoryTotal(category = "groceries", currency = "PKR", totalMinor = 20000L, count = 2),
            CategoryTotal(category = "fuel", currency = "PKR", totalMinor = 10000L, count = 1)
        )
        val summary = SpendSummaryBuilder.build(totals, baseCurrency = "PKR", rates = emptyMap())

        assertEquals(1, summary.byCurrency.size)
        assertEquals(30000L, summary.combinedTotalMinor)
        assertEquals(2, summary.combinedCategoryTotals?.size)
    }

    @Test
    fun mixed_currencies_without_rates_cannot_combine() {
        val totals = listOf(
            CategoryTotal(category = "groceries", currency = "PKR", totalMinor = 20000L, count = 2),
            CategoryTotal(category = "dining", currency = "USD", totalMinor = 5000L, count = 1)
        )
        val summary = SpendSummaryBuilder.build(totals, baseCurrency = "PKR", rates = emptyMap())

        assertEquals(2, summary.byCurrency.size)
        assertNull(summary.combinedTotalMinor)
        assertNull(summary.combinedCategoryTotals)
    }

    @Test
    fun mixed_currencies_with_full_rate_coverage_combines_correctly() {
        val totals = listOf(
            CategoryTotal(category = "groceries", currency = "PKR", totalMinor = 20000L, count = 2), // 200.00 PKR
            CategoryTotal(category = "dining", currency = "USD", totalMinor = 5000L, count = 1)       // 50.00 USD
        )
        // 1 USD = 278.5 PKR -> 50.00 USD = 13925.00 PKR = 1392500 minor
        val summary = SpendSummaryBuilder.build(totals, baseCurrency = "PKR", rates = mapOf("USD" to 278.5))

        assertEquals(2, summary.byCurrency.size)
        assertEquals(20000L + 1392500L, summary.combinedTotalMinor)
        assertEquals(2, summary.combinedCategoryTotals?.size)
        val diningTotal = summary.combinedCategoryTotals?.first { it.category == "dining" }
        assertEquals(1392500L, diningTotal?.totalMinor)
    }

    @Test
    fun partial_rate_coverage_cannot_combine() {
        val totals = listOf(
            CategoryTotal(category = "groceries", currency = "PKR", totalMinor = 20000L, count = 2),
            CategoryTotal(category = "dining", currency = "USD", totalMinor = 5000L, count = 1),
            CategoryTotal(category = "shopping", currency = "EUR", totalMinor = 3000L, count = 1)
        )
        // Only USD has a rate — EUR is missing, so we can't safely combine.
        val summary = SpendSummaryBuilder.build(totals, baseCurrency = "PKR", rates = mapOf("USD" to 278.5))

        assertEquals(3, summary.byCurrency.size)
        assertNull(summary.combinedTotalMinor)
    }

    @Test
    fun zero_and_negative_totals_are_excluded() {
        val totals = listOf(
            CategoryTotal(category = "groceries", currency = "PKR", totalMinor = 0L, count = 0),
            CategoryTotal(category = "fuel", currency = "PKR", totalMinor = 10000L, count = 1)
        )
        val summary = SpendSummaryBuilder.build(totals, baseCurrency = "PKR", rates = emptyMap())

        assertEquals(1, summary.byCurrency.size)
        assertEquals(10000L, summary.combinedTotalMinor)
    }

    @Test
    fun empty_totals_produce_empty_summary() {
        val summary = SpendSummaryBuilder.build(emptyList(), baseCurrency = "PKR", rates = emptyMap())
        assertEquals(0, summary.byCurrency.size)
        assertNull(summary.combinedTotalMinor)
    }

    @Test
    fun null_currency_rows_are_treated_as_base_currency() {
        val totals = listOf(
            CategoryTotal(category = "groceries", currency = null, totalMinor = 15000L, count = 1)
        )
        val summary = SpendSummaryBuilder.build(totals, baseCurrency = "PKR", rates = emptyMap())

        assertEquals(1, summary.byCurrency.size)
        assertEquals("PKR", summary.byCurrency.first().currency)
        assertEquals(15000L, summary.combinedTotalMinor)
    }

    @Test
    fun same_category_across_currencies_is_summed_after_conversion() {
        val totals = listOf(
            CategoryTotal(category = "dining", currency = "PKR", totalMinor = 10000L, count = 1),
            CategoryTotal(category = "dining", currency = "USD", totalMinor = 1000L, count = 1) // 10.00 USD
        )
        // 1 USD = 278.5 PKR -> 10.00 USD = 2785.00 PKR = 278500 minor
        val summary = SpendSummaryBuilder.build(totals, baseCurrency = "PKR", rates = mapOf("USD" to 278.5))

        assertEquals(1, summary.combinedCategoryTotals?.size) // merged into one "dining" entry
        assertEquals(10000L + 278500L, summary.combinedCategoryTotals?.first()?.totalMinor)
    }
}
