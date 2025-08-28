package com.mmushtaq.smartreceiptscanner.util

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Build
import android.print.pdf.PrintedPdfDocument // not used but nice for reference
import android.util.Log
import android.graphics.pdf.PdfRenderer
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.createBitmap

object PdfUtil {

    /**
     * Renders the first page of a PDF Uri to a high-quality JPEG in cache and returns a content Uri.
     * @param maxWidth target width in px for the rendered page (keeps aspect ratio).
     *                 1600–2200 is a good range for OCR; higher = slower/more memory.
     */
    fun renderFirstPageToCacheImage(
        context: Context,
        pdfUri: Uri,
        maxWidth: Int = 2000,
        jpegQuality: Int = 92
    ): Uri? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r") ?: return null
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) return null

            renderer.openPage(0).use { page ->
                // PdfRenderer gives logical page width/height in pixels at 72dpi-ish baseline.
                val pageW = page.width
                val pageH = page.height
                if (pageW <= 0 || pageH <= 0) return null

                val scale = (maxWidth.toFloat() / pageW.toFloat()).coerceAtLeast(1f)
                val outW = (pageW * scale).toInt()
                val outH = (pageH * scale).toInt()

                val bmp = createBitmap(outW, outH)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE) // white background for better OCR
                // Render the page
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                // Save to cache as JPEG
                val outFile = File(context.cacheDir, "pdf_first_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outFile).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos)
                }
                bmp.recycle()

                // Return a FileProvider Uri (you already have a provider with <cache-path>)
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fp",
                    outFile
                )
            }
        } catch (t: Throwable) {
            Log.e("PdfUtil", "PDF render failed", t)
            return null
        } finally {
            try { renderer?.close() } catch (_: Throwable) {}
            try { pfd?.close() } catch (_: Throwable) {}
        }
    }
}
