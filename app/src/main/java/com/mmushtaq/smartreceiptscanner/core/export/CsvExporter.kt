package com.mmushtaq.smartreceiptscanner.core.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.mmushtaq.smartreceiptscanner.core.data.Categories
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import com.mmushtaq.smartreceiptscanner.core.util.formatMinorPlain
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private const val AUTHORITY_SUFFIX = ".fp"

    /** Pure CSV generation — no Context/Uri involved, safe to unit test directly. */
    fun toCsvString(receipts: List<ReceiptEntity>): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        sb.appendLine(listOf("Merchant", "Date", "Total", "Currency", "Category").joinToString(",") { csvEscape(it) })
        receipts.forEach { r ->
            val row = listOf(
                r.merchant.orEmpty(),
                dateFmt.format(Date(r.dateEpochMs ?: r.createdAt)),
                r.totalMinor?.formatMinorPlain(r.currency).orEmpty(),
                r.currency.orEmpty(),
                r.category?.let { Categories.byId(it).label }.orEmpty()
            )
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
        fileName: String = defaultFileName()
    ): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(toCsvString(receipts))
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
}
