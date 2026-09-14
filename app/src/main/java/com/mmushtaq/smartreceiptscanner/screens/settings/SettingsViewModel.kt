package com.mmushtaq.smartreceiptscanner.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.export.JsonBackup
import com.mmushtaq.smartreceiptscanner.core.parser.MerchantPatternStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: ReceiptRepository,
    private val patternStore: MerchantPatternStore
) : ViewModel() {

    sealed interface BackupState {
        data object Idle : BackupState
        data object Working : BackupState
        data class BackupReady(val fileUri: Uri) : BackupState
        data class RestoreDone(val receiptsImported: Int, val patternsImported: Int) : BackupState
        data class Error(val message: String) : BackupState
    }

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state

    fun backup(context: Context) {
        _state.value = BackupState.Working
        viewModelScope.launch {
            runCatching {
                val receipts = repo.getAllOnce()
                val patterns = patternStore.getAllOnce()
                JsonBackup.export(context, receipts, patterns)
            }.onSuccess { uri -> _state.value = BackupState.BackupReady(uri) }
                .onFailure { _state.value = BackupState.Error("Backup failed: ${it.message}") }
        }
    }

    fun restore(context: Context, uri: Uri) {
        _state.value = BackupState.Working
        viewModelScope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Couldn't read the selected file")
                val payload = JsonBackup.parse(text)
                repo.importAll(payload.receipts)
                patternStore.importAll(payload.merchantPatterns)
                payload
            }.onSuccess { payload ->
                _state.value = BackupState.RestoreDone(payload.receipts.size, payload.merchantPatterns.size)
            }.onFailure {
                _state.value = BackupState.Error("Restore failed: ${it.message}")
            }
        }
    }

    fun reset() {
        _state.value = BackupState.Idle
    }
}
