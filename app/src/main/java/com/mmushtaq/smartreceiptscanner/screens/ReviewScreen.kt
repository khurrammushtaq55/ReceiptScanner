package com.mmushtaq.smartreceiptscanner.screens

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.scale
import com.mmushtaq.smartreceiptscanner.core.util.formatMinor
import com.mmushtaq.smartreceiptscanner.core.util.parseAmountInputToMinor
import com.mmushtaq.smartreceiptscanner.scan.OcrViewModel
import java.text.SimpleDateFormat
import java.util.Locale

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Review", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Captured Receipt",
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

        when (val s = vm.state.collectAsState().value) {
            OcrViewModel.UiState.Idle, OcrViewModel.UiState.Loading -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            is OcrViewModel.UiState.Error -> {
                Text("OCR Error: ${s.message}")
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

                // fields
                Text("Details", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (dd MMM yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it.uppercase(Locale.ROOT).take(3) },
                        label = { Text("Currency") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it },
                        label = { Text("Total") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Raw OCR Text", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    Text(
                        s.rawText.ifBlank { "(No text detected)" },
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        // push edits into VM
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
                                totalMinor = totalMinor
                            )
                        }
                        vm.save(imageUri)
                    }, enabled = saveEnabled) { Text("Save") }
                }
            }

            is OcrViewModel.UiState.Saved -> onDone()
        }
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
