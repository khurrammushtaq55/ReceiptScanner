package com.mmushtaq.smartreceiptscanner.core.data

/**
 * Pure minor-unit currency math — no Android dependencies, safe to unit test directly.
 * A "rate" is defined as: units of the base currency equal to 1 unit of the other currency
 * (e.g. rate("USD") = 278.5 means 1 USD = 278.5 PKR when PKR is the base).
 */
object CurrencyConverter {
    fun convertToBase(minorAmount: Long, rateUnitsOfBasePerUnit: Double): Long {
        val majorAmount = minorAmount / 100.0
        return Math.round(majorAmount * rateUnitsOfBasePerUnit * 100.0)
    }
}
