package com.mmushtaq.smartreceiptscanner.core.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.mmushtaq.smartreceiptscanner.core.data.Categories
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

    fun export(
        context: Context,
        receipts: List<ReceiptEntity>,
        title: String = "Receipt Report",
        fileName: String = defaultFileName()
    ): Uri {
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val document = PdfDocument()

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true; color = Color.DKGRAY }
        val cellPaint = Paint().apply { textSize = 11f; color = Color.BLACK }
        val mutedPaint = Paint().apply { textSize = 9f; color = Color.GRAY }

        val colX = floatArrayOf(MARGIN, MARGIN + 170f, MARGIN + 260f, MARGIN + 340f, MARGIN + 410f)
        val headers = listOf("Merchant", "Date", "Total", "Currency", "Category")

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

        var grandTotal = 0L
        receipts.forEach { r ->
            if (y > PAGE_HEIGHT - MARGIN - ROW_HEIGHT) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
                drawTableHeader()
            }
            val row = listOf(
                (r.merchant ?: "—").take(28),
                dateFmt.format(Date(r.dateEpochMs ?: r.createdAt)),
                r.totalMinor?.formatMinorPlain(r.currency) ?: "—",
                r.currency ?: "—",
                r.category?.let { Categories.byId(it).label } ?: "—"
            )
            row.forEachIndexed { i, v -> canvas.drawText(v, colX[i], y, cellPaint) }
            grandTotal += r.totalMinor ?: 0L
            y += ROW_HEIGHT
        }

        y += 14f
        canvas.drawText(
            "Total: ${grandTotal.formatMinorPlain(receipts.firstOrNull()?.currency)}",
            MARGIN, y, headerPaint
        )

        document.finishPage(page)

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
    }

    fun defaultFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "receipt_report_$stamp.pdf"
    }
}
