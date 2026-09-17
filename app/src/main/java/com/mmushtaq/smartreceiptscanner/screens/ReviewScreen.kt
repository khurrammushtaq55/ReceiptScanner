package com.mmushtaq.smartreceiptscanner.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import com.mmushtaq.smartreceiptscanner.R
import com.mmushtaq.smartreceiptscanner.core.data.Categories
import com.mmushtaq.smartreceiptscanner.core.parser.ReceiptParser
import com.mmushtaq.smartreceiptscanner.core.util.formatMinor
import com.mmushtaq.smartreceiptscanner.core.util.parseAmountInputToMinor
import androidx.compose.ui.tooling.preview.Preview
import com.mmushtaq.smartreceiptscanner.scan.OcrViewModel
import java.text.SimpleDateFormat
import java.util.Locale

import androidx.compose.material3.MaterialTheme
import org.koin.androidx.compose.koinViewModel
import java.util.Date

@Composable
fun ReviewScreen(
    imageUri: Uri,
    contentResolver: ContentResolver,
    onDone: () -> Unit,
    vm: OcrViewModel = koinViewModel(),
) {
    // downscaled preview bitmap
    val bmp by remember(imageUri) { mutableStateOf(loadPreviewBitmap(contentResolver, imageUri)) }
    LaunchedEffect(imageUri) { vm.runOcr(imageUri) }

    val state by vm.state.collectAsState()

    ReviewContent(
        bmp = bmp,
        state = state,
        onDone = onDone,
        onSave = { merchant, dateText, currency, totalText, category ->
            val parsedDate = runCatching {
                if (dateText.isBlank()) null
                else SimpleDateFormat(
                    "dd MMM yyyy",
                    Locale.getDefault()
                ).parse(dateText)?.time
            }.getOrNull()

            val totalMinor = parseAmountInputToMinor(totalText, currency)

            vm.updateEdit {
                it.copy(
                    merchant = merchant,
                    dateEpochMs = parsedDate,
                    currency = currency.ifBlank { "PKR" },
                    totalMinor = totalMinor,
                    category = category
                )
            }
            vm.save(imageUri)
        }
    )
}

@Composable
fun ReviewContent(
    bmp: Bitmap?,
    state: OcrViewModel.UiState,
    onDone: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.review), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = stringResource(R.string.captured_receipt),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(
            Modifier
                .fillMaxWidth()
                .height(2.dp),
            thickness = 2.dp,
            color = Color.Gray
        )
        Spacer(Modifier.height(12.dp))

        when (val s = state) {
            OcrViewModel.UiState.Idle, OcrViewModel.UiState.Loading -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            is OcrViewModel.UiState.Error -> {
                Text(stringResource(R.string.ocr_error, s.message))
            }

            is OcrViewModel.UiState.Success -> {
                val edit = s.edit
                var merchant by remember { mutableStateOf(edit.merchant) }
                var currency by remember { mutableStateOf(edit.currency) }
                var dateText by remember {
                    mutableStateOf(edit.dateEpochMs?.let {
                        SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(Date(it))
                    }.orEmpty())
                }
                var totalText by remember {
                    mutableStateOf(edit.totalMinor?.let { (it).formatMinor(currency) }
                        ?.substringBeforeLast(' ') ?: "")
                }
                var saveEnabled by remember { mutableStateOf(true) }
                var category by remember { mutableStateOf(edit.category ?: Categories.Other.id) }

                val confidence = s.confidence
                fun isLow(field: String) =
                    (confidence[field] ?: 1f) < ReceiptParser.LOW_CONFIDENCE_THRESHOLD

                // fields
                Text(stringResource(R.string.details), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text(stringResource(R.string.merchant)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                VerifyHint(isLow(ReceiptParser.Field.MERCHANT))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text(stringResource(R.string.date_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                VerifyHint(isLow(ReceiptParser.Field.DATE))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it.uppercase(Locale.ROOT).take(3) },
                            label = { Text(stringResource(R.string.currency)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        VerifyHint(isLow(ReceiptParser.Field.CURRENCY))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = totalText,
                            onValueChange = { totalText = it },
                            label = { Text(stringResource(R.string.total)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        VerifyHint(isLow(ReceiptParser.Field.TOTAL))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.category), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Categories.all) { cat ->
                        FilterChip(
                            selected = category == cat.id,
                            onClick = { category = cat.id },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.raw_ocr_text), fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Box(Modifier
                    .fillMaxWidth()) {
                    Text(
                        s.rawText.ifBlank { stringResource(R.string.no_text_detected) }
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onSave(merchant, dateText, currency, totalText, category)
                    }, enabled = saveEnabled) { Text(stringResource(R.string.save)) }
                }
            }

            is OcrViewModel.UiState.Saved -> LaunchedEffect(Unit) { onDone() }
        }
    }
}
@Composable
private fun VerifyHint(show: Boolean) {
    if (show) {
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.low_confidence_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB26A00) // amber, distinct from the theme's error color
        )
    }
}

fun loadPreviewBitmap(resolver: ContentResolver, uri: Uri): Bitmap? {
    return try {
        resolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, opts)
            val target = 1280 // px long edge for preview
            val sample = (opts.outWidth.coerceAtLeast(opts.outHeight) / target).coerceAtLeast(1)
            val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { inp2 ->
                BitmapFactory.decodeStream(inp2, null, opts2)?.let { bmp ->
                    // ensure reasonable width for UI
                    val w = bmp.width;
                    val h = bmp.height
                    val maxW = 1600
                    if (w > maxW) bmp.scale(maxW, (h * (maxW / w.toFloat())).toInt(), true) else bmp
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
fun ReviewContentPreview() {
    MaterialTheme {
        ReviewContent(
            bmp = null,
            state = OcrViewModel.UiState.Success(
                rawText = "SAMPLE RECEIPT TEXT\nSTORE #123\nTOTAL $10.00",
                edit = OcrViewModel.EditModel(
                    merchant = "Store #123",
                    dateEpochMs = System.currentTimeMillis(),
                    currency = "USD",
                    totalMinor = 1000L,
                    taxMinor = 0L,
                    category = Categories.Groceries.id
                )
            ),
            onDone = {},
            onSave = { _, _, _, _, _ -> }
        )
    }
}
