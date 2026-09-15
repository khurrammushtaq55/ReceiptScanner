package com.mmushtaq.smartreceiptscanner.core.data

import com.mmushtaq.smartreceiptscanner.core.data.db.CategoryTotal

data class CurrencyGroup(
    val currency: String,
    val totalMinor: Long,
    val categories: List<CategoryTotal>
)

data class CategoryAmount(val category: String?, val totalMinor: Long)

data class SpendSummary(
    val byCurrency: List<CurrencyGroup>,
    /** Non-null only when every currency present is either the base currency or has a known rate. */
    val combinedTotalMinor: Long?,
    val combinedCategoryTotals: List<CategoryAmount>?,
    val baseCurrency: String
)

/**
 * Pure aggregation logic — no Android dependencies, safe to unit test directly.
 * [rates]: currency code -> units of [baseCurrency] equal to 1 unit of that currency.
 */
object SpendSummaryBuilder {

    fun build(
        totals: List<CategoryTotal>,
        baseCurrency: String,
        rates: Map<String, Double>
    ): SpendSummary {
        val base = baseCurrency.trim().uppercase()
        val spent = totals.filter { it.totalMinor > 0 }

        val byCurrency = spent
            .groupBy { (it.currency ?: base).uppercase() }
            .map { (currency, rows) -> CurrencyGroup(currency, rows.sumOf { it.totalMinor }, rows) }
            .sortedByDescending { it.totalMinor }

        val allConvertible = byCurrency.all { it.currency == base || rates.containsKey(it.currency) }

        var combinedTotal: Long? = null
        var combinedCategories: List<CategoryAmount>? = null

        if (allConvertible && byCurrency.isNotEmpty()) {
            val perCategory = linkedMapOf<String?, Long>()
            spent.forEach { row ->
                val currency = (row.currency ?: base).uppercase()
                val convertedMinor = if (currency == base) {
                    row.totalMinor
                } else {
                    CurrencyConverter.convertToBase(row.totalMinor, rates.getValue(currency))
                }
                perCategory[row.category] = (perCategory[row.category] ?: 0L) + convertedMinor
            }
            combinedCategories = perCategory.entries
                .map { (cat, minor) -> CategoryAmount(cat, minor) }
                .sortedByDescending { it.totalMinor }
            combinedTotal = combinedCategories.sumOf { it.totalMinor }
        }

        return SpendSummary(byCurrency, combinedTotal, combinedCategories, base)
    }
}
