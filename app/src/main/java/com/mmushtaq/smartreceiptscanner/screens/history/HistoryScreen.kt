package com.mmushtaq.smartreceiptscanner.screens.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import com.mmushtaq.smartreceiptscanner.core.util.formatMinor
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (ReceiptEntity) -> Unit,
    vm: HistoryViewModel = koinViewModel()
) {
    val items by vm.ui.collectAsState()
    var search by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier
            .padding(pad)
            .fillMaxSize()) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it; vm.setQuery(it) },
                label = { Text("Search merchant or text…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                singleLine = true
            )

            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No receipts yet. Scan or import to see them here.")
                }
            } else {
                val receipts by vm.ui.collectAsState()

                LazyColumn {
                    items(count = receipts.size, key = { receipts[it].id }) { index ->

                        val r = receipts[index]
                        ReceiptRow(r) { onOpenDetail(r) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(r: ReceiptEntity, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier
            .height(IntrinsicSize.Min)
            .padding(12.dp)) {
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
    return total ?: (r.rawText.take(60).replace("\n", " ") + if (r.rawText.length > 60) "…" else "")
}



fun Long.formatDate(
    pattern: String = "dd MMM yyyy",
    locale: Locale = Locale.getDefault()
): String = SimpleDateFormat(pattern, locale).format(Date(this))