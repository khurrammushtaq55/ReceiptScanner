package com.mmushtaq.smartreceiptscanner.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import kotlinx.coroutines.flow.*

class HistoryViewModel(private val repo: ReceiptRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val source = repo.observeReceipts()

    val ui: StateFlow<List<ReceiptEntity>> = combine(source, query) { list, q ->
        if (q.isBlank()) list
        else {
            val needle = q.trim().lowercase()
            list.filter { e ->
                (e.merchant?.lowercase()?.contains(needle) == true) ||
                        e.rawText.lowercase().contains(needle)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setQuery(text: String) { query.value = text }
}
