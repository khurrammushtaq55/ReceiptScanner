package com.mmushtaq.smartreceiptscanner.screens.history

import com.mmushtaq.smartreceiptscanner.core.data.RateSource
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.data.db.CategoryTotal
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptDao
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** In-memory fake — ReceiptDao is a Room DAO, not unit-testable directly. */
private class FakeReceiptDao(initial: List<ReceiptEntity> = emptyList()) : ReceiptDao {
    val state = MutableStateFlow(initial)
    override fun observeAll(): Flow<List<ReceiptEntity>> = state
    override suspend fun getAllOnce(): List<ReceiptEntity> = state.value
    override suspend fun insert(entity: ReceiptEntity) {
        state.value = state.value.filterNot { it.id == entity.id } + entity
    }
    override suspend fun insertAll(entities: List<ReceiptEntity>) {
        val ids = entities.map { it.id }.toSet()
        state.value = state.value.filterNot { it.id in ids } + entities
    }
    override suspend fun get(id: String): ReceiptEntity? = state.value.firstOrNull { it.id == id }
    override suspend fun delete(entity: ReceiptEntity) {
        state.value = state.value.filterNot { it.id == entity.id }
    }
    override fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>> =
        MutableStateFlow(emptyList())
}

private class FakeRateSource(
    base: String = "PKR",
    rateMap: Map<String, Double> = emptyMap()
) : RateSource {
    override val baseCurrency: StateFlow<String> = MutableStateFlow(base)
    override val rates: StateFlow<Map<String, Double>> = MutableStateFlow(rateMap)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun receipt(
        id: String,
        merchant: String? = "Merchant",
        totalMinor: Long? = 1000L,
        category: String? = null,
        dateEpochMs: Long? = 1_700_000_000_000L,
        createdAt: Long = 1_700_000_000_000L
    ) = ReceiptEntity(
        id = id, imageUri = "uri", rawText = "raw $merchant",
        createdAt = createdAt, dateEpochMs = dateEpochMs,
        merchant = merchant, totalMinor = totalMinor, currency = "PKR", category = category
    )

    /** ui is SharingStarted.Lazily — .value never updates without an active collector. */
    private fun TestScope.startCollectingUi(vm: HistoryViewModel) {
        backgroundScope.launch { vm.ui.collect() }
    }

    @Test
    fun filters_by_search_query() = runTest(dispatcher) {
        val dao = FakeReceiptDao(listOf(receipt("1", merchant = "Al Faisal Mart"), receipt("2", merchant = "Corner Cafe")))
        val vm = HistoryViewModel(ReceiptRepository(dao), FakeRateSource())
        startCollectingUi(vm)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setQuery("cafe")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2"), vm.ui.value.map { it.id })
    }

    @Test
    fun filters_by_category() = runTest(dispatcher) {
        val dao = FakeReceiptDao(listOf(receipt("1", category = "groceries"), receipt("2", category = "dining")))
        val vm = HistoryViewModel(ReceiptRepository(dao), FakeRateSource())
        startCollectingUi(vm)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setCategoryFilter("dining")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2"), vm.ui.value.map { it.id })
    }

    @Test
    fun filters_by_amount_range() = runTest(dispatcher) {
        val dao = FakeReceiptDao(
            listOf(receipt("1", totalMinor = 500L), receipt("2", totalMinor = 1500L), receipt("3", totalMinor = 3000L))
        )
        val vm = HistoryViewModel(ReceiptRepository(dao), FakeRateSource())
        startCollectingUi(vm)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setAmountRange(1000L, 2000L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2"), vm.ui.value.map { it.id })
    }

    @Test
    fun filters_by_date_range() = runTest(dispatcher) {
        val dao = FakeReceiptDao(
            listOf(
                receipt("1", dateEpochMs = 1_000_000L),
                receipt("2", dateEpochMs = 2_000_000L),
                receipt("3", dateEpochMs = 3_000_000L)
            )
        )
        val vm = HistoryViewModel(ReceiptRepository(dao), FakeRateSource())
        startCollectingUi(vm)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setDateRange(1_500_000L, 2_500_000L)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2"), vm.ui.value.map { it.id })
    }

    @Test
    fun clearRangeFilters_resets_amount_and_date() = runTest(dispatcher) {
        val dao = FakeReceiptDao(listOf(receipt("1", totalMinor = 500L)))
        val vm = HistoryViewModel(ReceiptRepository(dao), FakeRateSource())
        startCollectingUi(vm)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setAmountRange(1000L, 2000L)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.ui.value.isEmpty())

        vm.clearRangeFilters()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("1"), vm.ui.value.map { it.id })
    }

    @Test
    fun selection_mode_tracks_selected_ids() = runTest(dispatcher) {
        val dao = FakeReceiptDao(listOf(receipt("1"), receipt("2")))
        val vm = HistoryViewModel(ReceiptRepository(dao), FakeRateSource())
        startCollectingUi(vm)
        dispatcher.scheduler.advanceUntilIdle()

        vm.enterSelectionMode("1")
        assertTrue(vm.selectionMode.value)
        assertEquals(setOf("1"), vm.selectedIds.value)

        vm.toggleSelected("2")
        assertEquals(setOf("1", "2"), vm.selectedIds.value)

        vm.toggleSelected("1")
        assertEquals(setOf("2"), vm.selectedIds.value)

        vm.exitSelectionMode()
        assertFalse(vm.selectionMode.value)
        assertTrue(vm.selectedIds.value.isEmpty())
    }

    @Test
    fun deleteSelected_removes_selected_receipts() = runTest(dispatcher) {
        val dao = FakeReceiptDao(listOf(receipt("1"), receipt("2"), receipt("3")))
        val vm = HistoryViewModel(ReceiptRepository(dao), FakeRateSource())
        startCollectingUi(vm)
        dispatcher.scheduler.advanceUntilIdle()

        vm.enterSelectionMode()
        vm.selectAll(listOf("1", "3"))
        vm.deleteSelected(vm.ui.value)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("2"), dao.state.value.map { it.id })
        assertFalse(vm.selectionMode.value)
    }
}
