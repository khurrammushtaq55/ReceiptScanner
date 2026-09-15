package com.mmushtaq.smartreceiptscanner.core.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.mmushtaq.smartreceiptscanner.core.data.Categories
import com.mmushtaq.smartreceiptscanner.core.data.CurrencyConverter
import com.mmushtaq.smartreceiptscanner.core.data.db.ReceiptEntity
import com.mmushtaq.smartreceiptscanner.core.util.formatMinorPlain
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Simple tabular PDF report — no external library needed, uses android.graphics.pdf.PdfDocument. */
object PdfReportExporter {

    private const val AUTHORITY_SUFFIX = ".fp"
    private const val PAGE_WIDTH = 595   // A4 @ 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val ROW_HEIGHT = 22f

    /**
     * When [baseCurrency] is provided, an extra "Converted" column is added — populated only for
     * rows whose currency is the base currency or has a known entry in [rates] — and the footer
     * shows one combined total. Otherwise (or for currencies the rates can't cover), the footer
     * shows a subtotal per currency rather than an incorrect single sum across currencies.
     */
    fun export(
        context: Context,
        receipts: List<ReceiptEntity>,
        title: String = "Receipt Report",
        fileName: String = defaultFileName(),
        baseCurrency: String? = null,
        rates: Map<String, Double> = emptyMap()
    ): Uri {
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val document = PdfDocument()

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true; color = Color.DKGRAY }
        val cellPaint = Paint().apply { textSize = 11f; color = Color.BLACK }
        val mutedPaint = Paint().apply { textSize = 9f; color = Color.GRAY }

        val showConverted = baseCurrency != null
        val colX = if (showConverted) {
            floatArrayOf(MARGIN, MARGIN + 150f, MARGIN + 230f, MARGIN + 300f, MARGIN + 360f, MARGIN + 440f)
        } else {
            floatArrayOf(MARGIN, MARGIN + 170f, MARGIN + 260f, MARGIN + 340f, MARGIN + 410f)
        }
        val headers = if (showConverted) {
            listOf("Merchant", "Date", "Total", "Currency", "Category", "Converted ($baseCurrency)")
        } else {
            listOf("Merchant", "Date", "Total", "Currency", "Category")
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        fun drawTableHeader() {
            headers.forEachIndexed { i, h -> canvas.drawText(h, colX[i], y, headerPaint) }
            y += ROW_HEIGHT
        }

        canvas.drawText(title, MARGIN, y, titlePaint)
        y += 24f
        canvas.drawText("Generated ${dateFmt.format(Date())} · ${receipts.size} receipt(s)", MARGIN, y, mutedPaint)
        y += 20f
        drawTableHeader()

        receipts.forEach { r ->
            if (y > PAGE_HEIGHT - MARGIN - ROW_HEIGHT) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
                drawTableHeader()
            }
            val row = mutableListOf(
                (r.merchant ?: "—").take(24),
                dateFmt.format(Date(r.dateEpochMs ?: r.createdAt)),
                r.totalMinor?.formatMinorPlain(r.currency) ?: "—",
                r.currency ?: "—",
                r.category?.let { Categories.byId(it).label } ?: "—"
            )
            if (showConverted) {
                val converted = CsvExporter.convertedTotalText(r.totalMinor, r.currency, baseCurrency!!, rates)
                row += converted.ifEmpty { "—" }
            }
            row.forEachIndexed { i, v -> canvas.drawText(v, colX[i], y, cellPaint) }
            y += ROW_HEIGHT
        }

        y += 14f
        drawFooterTotals(canvas, y, headerPaint, receipts, baseCurrency, rates)

        document.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
    }

    /**
     * Shows one combined total when every currency present is convertible (base currency or has a
     * rate); otherwise shows one line per currency so nothing gets silently summed incorrectly.
     */
    private fun drawFooterTotals(
        canvas: android.graphics.Canvas,
        startY: Float,
        paint: Paint,
        receipts: List<ReceiptEntity>,
        baseCurrency: String?,
        rates: Map<String, Double>
    ) {
        var y = startY
        val byCurrency = receipts
            .filter { it.totalMinor != null }
            .groupBy { (it.currency ?: "—").uppercase(Locale.ROOT) }
            .mapValues { (_, rows) -> rows.sumOf { it.totalMinor ?: 0L } }

        val base = baseCurrency?.uppercase(Locale.ROOT)
        val allConvertible = base != null && byCurrency.keys.all { it == base || rates.containsKey(it) }

        if (allConvertible) {
            val combined = byCurrency.entries.sumOf { (currency, minor) ->
                if (currency == base) minor else CurrencyConverter.convertToBase(minor, rates.getValue(currency))
            }
            canvas.drawText("Total: ${combined.formatMinorPlain(base)}", MARGIN, y, paint)
        } else {
            byCurrency.entries.sortedByDescending { it.value }.forEach { (currency, minor) ->
                canvas.drawText("Total ($currency): ${minor.formatMinorPlain(currency)}", MARGIN, y, paint)
                y += ROW_HEIGHT
            }
        }
    }

    fun defaultFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "receipt_report_$stamp.pdf"
    }
}
