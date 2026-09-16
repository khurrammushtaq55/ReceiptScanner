package com.mmushtaq.smartreceiptscanner.core.data

import kotlinx.coroutines.flow.StateFlow

/** Read-only view of exchange rate state — lets ViewModels depend on this instead of the
 * concrete, Context-backed [ExchangeRateStore], so they can be unit-tested with a fake. */
interface RateSource {
    val baseCurrency: StateFlow<String>
    val rates: StateFlow<Map<String, Double>>
}
