package com.mmushtaq.smartreceiptscanner.core.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mmushtaq.smartreceiptscanner.core.data.Categories
import com.mmushtaq.smartreceiptscanner.core.data.CurrencyConverter
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import com.mmushtaq.smartreceiptscanner.core.util.formatMinorPlain
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val AUTHORITY_SUFFIX = ".fp"

    /**
     * Pure CSV generation — no Context/Uri involved, safe to unit test directly.
     * When [baseCurrency] is provided, an extra "Converted Total" column is added — populated
     * only for rows whose currency is the base currency or has a known entry in [rates].
     */
    fun toCsvString(
        receipts: List<ReceiptEntity>,
        baseCurrency: String? = null,
        rates: Map<String, Double> = emptyMap()
    ): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        val headers = mutableListOf("Merchant", "Date", "Total", "Currency", "Category")
        if (baseCurrency != null) headers += "Converted Total ($baseCurrency)"
        sb.appendLine(headers.joinToString(",") { csvEscape(it) })

        receipts.forEach { r ->
            val row = mutableListOf(
                r.merchant.orEmpty(),
                dateFmt.format(Date(r.dateEpochMs ?: r.createdAt)),
                r.totalMinor?.formatMinorPlain(r.currency).orEmpty(),
                r.currency.orEmpty(),
                r.category?.let { Categories.byId(it).label }.orEmpty()
            )
            if (baseCurrency != null) {
                row += convertedTotalText(r.totalMinor, r.currency, baseCurrency, rates)
            }
            sb.appendLine(row.joinToString(",") { csvEscape(it) })
        }
        return sb.toString()
    }

    /**
     * Writes [receipts] to a CSV file under the app cache dir and returns a shareable
     * `content://` Uri (via FileProvider — no storage permission needed).
     */
    fun export(
        context: Context,
        receipts: List<ReceiptEntity>,
        fileName: String = defaultFileName(),
        baseCurrency: String? = null,
        rates: Map<String, Double> = emptyMap()
    ): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(toCsvString(receipts, baseCurrency, rates))
        return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
    }

    fun defaultFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "receipts_export_$stamp.csv"
    }

    /** Quotes a field if it contains a comma, quote, or newline; doubles embedded quotes. */
    fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    /** Blank when not convertible (no rate for that currency, or the row has no amount/currency). */
    fun convertedTotalText(
        minor: Long?,
        currency: String?,
        baseCurrency: String,
        rates: Map<String, Double>
    ): String {
        if (minor == null) return ""
        val code = currency?.uppercase(Locale.ROOT) ?: return ""
        val base = baseCurrency.uppercase(Locale.ROOT)
        val convertedMinor = when {
            code == base -> minor
            rates.containsKey(code) -> CurrencyConverter.convertToBase(minor, rates.getValue(code))
            else -> return ""
        }
        return convertedMinor.formatMinorPlain(base)
    }
}
