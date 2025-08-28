package com.mmushtaq.smartreceiptscanner.scan

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mmushtaq.smartreceiptscanner.core.data.ReceiptRepository
import com.mmushtaq.smartreceiptscanner.core.ocr.OcrClient
import com.mmushtaq.smartreceiptscanner.core.parser.ReceiptParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OcrViewModel(
    private val ocr: OcrClient,
    private val repo: ReceiptRepository
) : ViewModel() {

    data class EditModel(
        val merchant: String,
        val dateEpochMs: Long?,
        val currency: String,
        val totalMinor: Long?,
        val taxMinor: Long?
    )

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Success(val rawText: String, val edit: EditModel) : UiState
        data class Error(val message: String) : UiState
        data class Saved(val id: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    fun runOcr(uri: Uri, defaultCurrency: String = "PKR") {
        _state.value = UiState.Loading
        viewModelScope.launch {
            runCatching { ocr.recognize(uri).text }
                .onSuccess { text ->
                    val p = ReceiptParser.parse(text, defaultCurrency)
                    val edit = EditModel(
                        merchant = p.merchant.orEmpty(),
                        dateEpochMs = p.dateEpochMs,
                        currency = p.currency ?: defaultCurrency,
                        totalMinor = p.totalMinor,
                        taxMinor = p.taxMinor
                    )
                    _state.value = UiState.Success(text, edit)
                }
                .onFailure { _state.value = UiState.Error(it.message ?: "OCR failed") }
        }
    }

    fun updateEdit(transform: (EditModel) -> EditModel) {
        val cur = _state.value
        if (cur is UiState.Success) {
            _state.value = cur.copy(edit = transform(cur.edit))
        }
    }

    fun save(uri: Uri) {
        val cur = _state.value as? UiState.Success ?: return
        viewModelScope.launch {
            runCatching {
                repo.saveBasic(
                    imageUri = uri.toString(),
                    rawText = cur.rawText,
                    merchant = cur.edit.merchant
                ).also { id ->
                    // Immediately patch parsed fields
                    repo.updateParsed(
                        id = id,
                        merchant = cur.edit.merchant.ifBlank { null },
                        dateEpochMs = cur.edit.dateEpochMs,
                        currency = cur.edit.currency,
                        totalMinor = cur.edit.totalMinor,
                        taxMinor = cur.edit.taxMinor
                    )
                    id
                }
            }.onSuccess { _state.value = UiState.Saved(it) }
                .onFailure { _state.value = UiState.Error("Save failed: ${it.message}") }
        }
    }
}
