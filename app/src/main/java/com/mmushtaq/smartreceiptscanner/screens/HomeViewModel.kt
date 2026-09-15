package com.mmushtaq.smartreceiptscanner.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq.smartreceiptscanner.core.data.RateSource
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.data.db.CategoryTotal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    repo: ReceiptRepository,
    rateStore: RateSource
) : ViewModel() {
    val monthlyCategoryTotals: StateFlow<List<CategoryTotal>> =
        repo.observeMonthlyCategoryTotals()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val baseCurrency: StateFlow<String> = rateStore.baseCurrency
    val rates: StateFlow<Map<String, Double>> = rateStore.rates
}
