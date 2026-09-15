package com.mmushtaq.smartreceiptscanner.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq.smartreceiptscanner.core.data.RateSource
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Optional amount (minor units) and date (epoch millis) range filters. */
data class RangeFilters(
    val minAmountMinor: Long? = null,
    val maxAmountMinor: Long? = null,
    val dateFrom: Long? = null,
    val dateTo: Long? = null
) {
    val isActive: Boolean get() = minAmountMinor != null || maxAmountMinor != null || dateFrom != null || dateTo != null
}

class HistoryViewModel(
    private val repo: ReceiptRepository,
    rateStore: RateSource
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<String?>(null)
    private val rangeFilters = MutableStateFlow(RangeFilters())
    private val source = repo.observeReceipts()

    val baseCurrency: StateFlow<String> = rateStore.baseCurrency
    val rates: StateFlow<Map<String, Double>> = rateStore.rates

    val ui: StateFlow<List<ReceiptEntity>> = combine(source, query, categoryFilter, rangeFilters) { list, q, cat, range ->
        val needle = q.trim().lowercase()
        list.filter { e ->
            val matchesQuery = needle.isBlank() ||
                    (e.merchant?.lowercase()?.contains(needle) == true) ||
                    e.rawText.lowercase().contains(needle)
            val matchesCategory = cat == null || e.category == cat
            val matchesMin = range.minAmountMinor == null ||
                    (e.totalMinor != null && e.totalMinor >= range.minAmountMinor)
            val matchesMax = range.maxAmountMinor == null ||
                    (e.totalMinor != null && e.totalMinor <= range.maxAmountMinor)
            val effectiveDate = e.dateEpochMs ?: e.createdAt
            val matchesFrom = range.dateFrom == null || effectiveDate >= range.dateFrom
            val matchesTo = range.dateTo == null || effectiveDate <= range.dateTo
            matchesQuery && matchesCategory && matchesMin && matchesMax && matchesFrom && matchesTo
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // --- Filters ---
    fun setQuery(text: String) { query.value = text }
    fun setCategoryFilter(categoryId: String?) { categoryFilter.value = categoryId }
    fun setAmountRange(minMinor: Long?, maxMinor: Long?) {
        rangeFilters.value = rangeFilters.value.copy(minAmountMinor = minMinor, maxAmountMinor = maxMinor)
    }
    fun setDateRange(from: Long?, to: Long?) {
        rangeFilters.value = rangeFilters.value.copy(dateFrom = from, dateTo = to)
    }
    fun clearRangeFilters() { rangeFilters.value = RangeFilters() }
    val activeRangeFilters: StateFlow<RangeFilters> = rangeFilters

    // --- Multi-select / bulk actions ---
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds

    fun enterSelectionMode(initialId: String? = null) {
        _selectionMode.value = true
        if (initialId != null) _selectedIds.value = setOf(initialId)
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedIds.value = emptySet()
    }

    fun toggleSelected(id: String) {
        val cur = _selectedIds.value
        _selectedIds.value = if (cur.contains(id)) cur - id else cur + id
    }

    fun selectAll(ids: List<String>) { _selectedIds.value = ids.toSet() }

    fun deleteSelected(all: List<ReceiptEntity>) {
        viewModelScope.launch {
            val toDelete = all.filter { it.id in _selectedIds.value }
            toDelete.forEach { repo.delete(it) }
            exitSelectionMode()
        }
    }
}
