package com.mmushtaq.smartreceiptscanner.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.mmushtaq.smartreceiptscanner.R
import com.mmushtaq.smartreceiptscanner.ads.BannerAd
import com.mmushtaq.smartreceiptscanner.core.data.Categories
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import com.mmushtaq.smartreceiptscanner.core.export.CsvExporter
import com.mmushtaq.smartreceiptscanner.core.export.PdfReportExporter
import com.mmushtaq.smartreceiptscanner.core.export.ShareUtil
import com.mmushtaq.smartreceiptscanner.core.util.formatMinor
import androidx.compose.ui.tooling.preview.Preview
import com.mmushtaq.smartreceiptscanner.screens.history.RangeFilters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (ReceiptEntity) -> Unit,
    vm: HistoryViewModel = koinViewModel()
) {
    val items by vm.ui.collectAsState()
    val selectionMode by vm.selectionMode.collectAsState()
    val selectedIds by vm.selectedIds.collectAsState()
    val activeRange by vm.activeRangeFilters.collectAsState()
    val baseCurrency by vm.baseCurrency.collectAsState()
    val rates by vm.rates.collectAsState()

    HistoryContent(
        items = items,
        selectionMode = selectionMode,
        selectedIds = selectedIds,
        activeRange = activeRange,
        baseCurrency = baseCurrency,
        rates = rates,
        onBack = onBack,
        onOpenDetail = onOpenDetail,
        onSetQuery = vm::setQuery,
        onSetCategoryFilter = vm::setCategoryFilter,
        onSetAmountRange = vm::setAmountRange,
        onSetDateRange = vm::setDateRange,
        onClearRangeFilters = vm::clearRangeFilters,
        onToggleSelected = { vm.toggleSelected(it) },
        onSelectAll = { vm.selectAll(items.map { it.id }) },
        onDeleteSelected = { vm.deleteSelected(items) },
        onEnterSelectionMode = { vm.enterSelectionMode(it) },
        onExitSelectionMode = vm::exitSelectionMode
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    items: List<ReceiptEntity>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    activeRange: RangeFilters,
    baseCurrency: String,
    rates: Map<String, Double>,
    onBack: () -> Unit,
    onOpenDetail: (ReceiptEntity) -> Unit,
    onSetQuery: (String) -> Unit,
    onSetCategoryFilter: (String?) -> Unit,
    onSetAmountRange: (Long?, Long?) -> Unit,
    onSetDateRange: (Long?, Long?) -> Unit,
    onClearRangeFilters: () -> Unit,
    onToggleSelected: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEnterSelectionMode: (String?) -> Unit,
    onExitSelectionMode: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var exportMenuExpanded by remember { mutableStateOf(false) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun exportAndShare(toExport: List<ReceiptEntity>, asCsv: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (asCsv) {
                val uri = CsvExporter.export(context, toExport, baseCurrency = baseCurrency, rates = rates)
                withContext(Dispatchers.Main) {
                    ShareUtil.shareFile(context, uri, "text/csv", "Export receipts (CSV)")
                }
            } else {
                val uri = PdfReportExporter.export(context, toExport, baseCurrency = baseCurrency, rates = rates)
                withContext(Dispatchers.Main) {
                    ShareUtil.shareFile(context, uri, "application/pdf", "Export receipts (PDF)")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedIds.size} receipt(s)?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteSelected()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { onExitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onSelectAll() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                exportAndShare(items.filter { it.id in selectedIds }, asCsv = true)
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export selected as CSV")
                        }
                        IconButton(
                            enabled = selectedIds.isNotEmpty(),
                            onClick = { showDeleteConfirm = true }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("History") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                        IconButton(onClick = { exportMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = exportMenuExpanded,
                            onDismissRequest = { exportMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                enabled = items.isNotEmpty(),
                                onClick = {
                                    exportMenuExpanded = false
                                    exportAndShare(items, asCsv = true)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export PDF report") },
                                enabled = items.isNotEmpty(),
                                onClick = {
                                    exportMenuExpanded = false
                                    exportAndShare(items, asCsv = false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Select items") },
                                enabled = items.isNotEmpty(),
                                onClick = {
                                    exportMenuExpanded = false
                                    onEnterSelectionMode(null)
                                }
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            BannerAd()
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it; onSetQuery(it) },
                label = { Text(stringResource(R.string.search)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null; onSetCategoryFilter(null) },
                        label = { Text("All") }
                    )
                }
                items(Categories.all) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = { selectedCategory = cat.id; onSetCategoryFilter(cat.id) },
                        label = { Text(cat.label) }
                    )
                }
            }

            if (filtersExpanded) {
                RangeFiltersPanel(
                    initial = activeRange,
                    onApply = { min, max, from, to ->
                        onSetAmountRange(min, max)
                        onSetDateRange(from, to)
                    },
                    onClear = { onClearRangeFilters() }
                )
            }

            CurrencySubtotals(items)

            Spacer(Modifier.height(4.dp))

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_receipts_yet))
                }
            } else {
                LazyColumn {
                    items(count = items.size, key = { items[it].id }) { index ->
                        val r = items[index]
                        ReceiptRow(
                            r = r,
                            selectionMode = selectionMode,
                            selected = r.id in selectedIds,
                            onClick = {
                                if (selectionMode) onToggleSelected(r.id) else onOpenDetail(r)
                            },
                            onLongClick = {
                                if (!selectionMode) onEnterSelectionMode(r.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Subtotal per currency for the currently filtered [items] — avoids implying a single-currency total. */
@Composable
private fun CurrencySubtotals(items: List<ReceiptEntity>) {
    val subtotals = remember(items) {
        items
            .filter { it.totalMinor != null }
            .groupBy { it.currency ?: "—" }
            .mapValues { (_, rows) -> rows.sumOf { it.totalMinor ?: 0L } }
            .entries
            .sortedByDescending { it.value }
    }
    if (subtotals.isEmpty()) return

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        subtotals.forEach { (currency, total) ->
            Text(
                total.formatMinor(currency),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RangeFiltersPanel(
    initial: RangeFilters,
    onApply: (minMinor: Long?, maxMinor: Long?, from: Long?, to: Long?) -> Unit,
    onClear: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    var minText by remember { mutableStateOf(initial.minAmountMinor?.let { (it / 100.0).toString() }.orEmpty()) }
    var maxText by remember { mutableStateOf(initial.maxAmountMinor?.let { (it / 100.0).toString() }.orEmpty()) }
    var fromText by remember { mutableStateOf(initial.dateFrom?.let { dateFmt.format(Date(it)) }.orEmpty()) }
    var toText by remember { mutableStateOf(initial.dateTo?.let { dateFmt.format(Date(it)) }.orEmpty()) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Filters", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = { minText = it },
                    label = { Text("Min amount") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { maxText = it },
                    label = { Text("Max amount") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = fromText,
                    onValueChange = { fromText = it },
                    label = { Text("From (dd MMM yyyy)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = toText,
                    onValueChange = { toText = it },
                    label = { Text("To (dd MMM yyyy)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = {
                    minText = ""; maxText = ""; fromText = ""; toText = ""
                    onClear()
                }) { Text("Clear") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    val min = minText.toDoubleOrNull()?.let { Math.round(it * 100) }
                    val max = maxText.toDoubleOrNull()?.let { Math.round(it * 100) }
                    val from = runCatching { dateFmt.parse(fromText)?.time }.getOrNull()
                    val to = runCatching { dateFmt.parse(toText)?.time }.getOrNull()
                    onApply(min, max, from, to)
                }) { Text("Apply") }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceiptRow(
    r: ReceiptEntity,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            Modifier
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
                Spacer(Modifier.width(4.dp))
            }
            Image(
                painter = rememberAsyncImagePainter(r.imageUri),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.merchant ?: "Unknown merchant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    summaryLine(r),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                r.createdAt.formatDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun summaryLine(r: ReceiptEntity): String {
    val total = r.totalMinor?.formatMinor(r.currency)
    val catLabel = r.category?.let { Categories.byId(it).label }
    val base = total ?: (r.rawText.take(60).replace("\n", " ") + if (r.rawText.length > 60) "…" else "")
    return if (catLabel != null) "$catLabel • $base" else base
}

fun Long.formatDate(
    pattern: String = "dd MMM yyyy",
    locale: Locale = Locale.getDefault()
): String = SimpleDateFormat(pattern, locale).format(Date(this))

@Preview(showBackground = true)
@Composable
fun HistoryContentPreview() {
    MaterialTheme {
        HistoryContent(
            items = listOf(
                ReceiptEntity(
                    id = "1",
                    imageUri = "",
                    rawText = "Receipt 1",
                    createdAt = System.currentTimeMillis(),
                    dateEpochMs = System.currentTimeMillis(),
                    merchant = "Store A",
                    totalMinor = 1234L,
                    currency = "USD",
                    category = Categories.Groceries.id
                )
            ),
            selectionMode = false,
            selectedIds = emptySet(),
            activeRange = RangeFilters(),
            baseCurrency = "USD",
            rates = emptyMap(),
            onBack = {},
            onOpenDetail = {},
            onSetQuery = {},
            onSetCategoryFilter = {},
            onSetAmountRange = { _, _ -> },
            onSetDateRange = { _, _ -> },
            onClearRangeFilters = {},
            onToggleSelected = {},
            onSelectAll = {},
            onDeleteSelected = {},
            onEnterSelectionMode = {},
            onExitSelectionMode = {}
        )
    }
}
