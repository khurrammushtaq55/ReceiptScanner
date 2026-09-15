package com.mmushtaq.smartreceiptscanner.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmushtaq.smartreceiptscanner.R
import com.mmushtaq.smartreceiptscanner.core.export.ShareUtil
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val baseCurrency by vm.baseCurrency.collectAsState()
    val rates by vm.rates.collectAsState()

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.restore(context, it) } }

    // Once a backup file is written, hand it straight to the share sheet, then reset.
    LaunchedEffect(state) {
        val s = state
        if (s is SettingsViewModel.BackupState.BackupReady) {
            ShareUtil.shareFile(context, s.fileUri, "application/json", "Save backup")
            vm.reset()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Creates a local JSON file with all your receipts and learned merchant corrections. " +
                    "Nothing leaves your device automatically — you choose where to save or send it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            val working = state is SettingsViewModel.BackupState.Working

            Button(
                onClick = { vm.backup(context) },
                enabled = !working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Backup to JSON") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                enabled = !working,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore from JSON") }

            Spacer(Modifier.height(20.dp))

            when (val s = state) {
                is SettingsViewModel.BackupState.Working -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Working…")
                    }
                }

                is SettingsViewModel.BackupState.RestoreDone -> {
                    Text("Restored ${s.receiptsImported} receipt(s) and ${s.patternsImported} merchant pattern(s).")
                }

                is SettingsViewModel.BackupState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }

                else -> {}
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            ExchangeRatesSection(
                baseCurrency = baseCurrency,
                rates = rates,
                onSetBaseCurrency = vm::setBaseCurrency,
                onSetRate = vm::setRate,
                onRemoveRate = vm::removeRate
            )
        }
    }
}

@Composable
private fun ExchangeRatesSection(
    baseCurrency: String,
    rates: Map<String, Double>,
    onSetBaseCurrency: (String) -> Unit,
    onSetRate: (String, Double) -> Unit,
    onRemoveRate: (String) -> Unit
) {
    Text("Exchange Rates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
    Text(
        "Manually set conversion rates so receipts in different currencies can be combined into " +
            "one total on the Home screen. Nothing is fetched automatically.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(16.dp))

    var baseInput by remember(baseCurrency) { mutableStateOf(baseCurrency) }
    OutlinedTextField(
        value = baseInput,
        onValueChange = { baseInput = it.uppercase().take(3) },
        label = { Text("Base currency") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { onSetBaseCurrency(baseInput) },
        enabled = baseInput.length == 3 && baseInput != baseCurrency,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Save base currency") }

    Spacer(Modifier.height(20.dp))

    if (rates.isEmpty()) {
        Text(
            "No rates added yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Text("1 unit = X $baseCurrency", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        rates.entries.sortedBy { it.key }.forEach { (code, rate) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$code → $rate", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { onRemoveRate(code) }) {
                    Icon(Icons.Default.Close, contentDescription = "Remove $code rate")
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    var newCode by remember { mutableStateOf("") }
    var newRate by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = newCode,
            onValueChange = { newCode = it.uppercase().take(3) },
            label = { Text("Code") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = newRate,
            onValueChange = { newRate = it },
            label = { Text("Rate") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    val parsedRate = newRate.toDoubleOrNull()
    Button(
        onClick = {
            val r = parsedRate
            if (newCode.length == 3 && r != null && r > 0) {
                onSetRate(newCode, r)
                newCode = ""
                newRate = ""
            }
        },
        enabled = newCode.length == 3 && parsedRate != null && parsedRate > 0,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Add rate") }
}
