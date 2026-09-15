package com.mmushtaq.smartreceiptscanner.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * User-entered exchange rates for combining receipts across currencies into one figure.
 * Purely manual and local — no network calls, nothing fetched automatically.
 */
class ExchangeRateStore(context: Context) : RateSource {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _baseCurrency = MutableStateFlow(prefs.getString(KEY_BASE, DEFAULT_BASE) ?: DEFAULT_BASE)
    override val baseCurrency: StateFlow<String> = _baseCurrency

    /** currency code -> units of [baseCurrency] equal to 1 unit of that currency. */
    private val _rates = MutableStateFlow(RateSerialization.deserialize(prefs.getString(KEY_RATES, null)))
    override val rates: StateFlow<Map<String, Double>> = _rates

    fun setBaseCurrency(code: String) {
        val normalized = code.trim().uppercase()
        if (normalized.length != 3) return
        prefs.edit().putString(KEY_BASE, normalized).apply()
        _baseCurrency.value = normalized
    }

    fun setRate(currency: String, unitsOfBasePerUnit: Double) {
        val normalized = currency.trim().uppercase()
        if (normalized.length != 3 || unitsOfBasePerUnit <= 0) return
        val updated = _rates.value.toMutableMap().apply { put(normalized, unitsOfBasePerUnit) }
        persist(updated)
    }

    fun removeRate(currency: String) {
        val updated = _rates.value.toMutableMap().apply { remove(currency.trim().uppercase()) }
        persist(updated)
    }

    /** Converts [minor] units of [currency] into minor units of the base currency, or null if not possible. */
    fun convertToBase(minor: Long, currency: String?): Long? {
        val code = currency?.trim()?.uppercase() ?: return null
        if (code == _baseCurrency.value) return minor
        val rate = _rates.value[code] ?: return null
        return CurrencyConverter.convertToBase(minor, rate)
    }

    private fun persist(rates: Map<String, Double>) {
        prefs.edit().putString(KEY_RATES, RateSerialization.serialize(rates)).apply()
        _rates.value = rates
    }

    companion object {
        private const val PREFS_NAME = "exchange_rates"
        private const val KEY_BASE = "base_currency"
        private const val KEY_RATES = "rates"
        const val DEFAULT_BASE = "PKR"
    }
}
