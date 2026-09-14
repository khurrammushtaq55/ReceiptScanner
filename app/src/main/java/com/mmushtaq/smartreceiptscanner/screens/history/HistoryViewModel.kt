package com.mmushtaq.smartreceiptscanner.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import kotlinx.coroutines.flow.*

class HistoryViewModel(private val repo: ReceiptRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<String?>(null)
    private val source = repo.observeReceipts()

    val ui: StateFlow<List<ReceiptEntity>> = combine(source, query, categoryFilter) { list, q, cat ->
        val needle = q.trim().lowercase()
        list.filter { e ->
            val matchesQuery = needle.isBlank() ||
                    (e.merchant?.lowercase()?.contains(needle) == true) ||
                    e.rawText.lowercase().contains(needle)
            val matchesCategory = cat == null || e.category == cat
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setQuery(text: String) { query.value = text }
    fun setCategoryFilter(categoryId: String?) { categoryFilter.value = categoryId }
}
