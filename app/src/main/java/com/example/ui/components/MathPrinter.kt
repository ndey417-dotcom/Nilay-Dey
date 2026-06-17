package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.pdf.PrintedPdfDocument
import java.io.FileOutputStream
import java.io.IOException

class MathPrinter(
    private val context: Context,
    private val title: String,
    private val pagesContent: List<String>
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)

        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        if (pagesContent.isEmpty()) {
            callback.onLayoutFailed("No content to print")
            return
        }

        val info = PrintDocumentInfo.Builder("$title.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pagesContent.size)
            .build()

        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val pdf = pdfDocument ?: return

        for (i in pagesContent.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, i).create() // standard PDF dimension ratios
            val page = pdf.startPage(pageInfo)

            if (cancellationSignal?.isCanceled == true) {
                pdfDocument = null
                callback.onWriteCancelled()
                return
            }

            drawNotebookPage(page.canvas, pagesContent[i], i + 1)
            pdf.finishPage(page)
        }

        try {
            pdf.writeTo(FileOutputStream(destination.fileDescriptor))
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: IOException) {
            callback.onWriteFailed(e.toString())
        } finally {
            pdf.close()
            pdfDocument = null
        }
    }

    private fun drawNotebookPage(canvas: Canvas, content: String, pageNumber: Int) {
        val bgPaint = Paint().apply { color = 0xFFFFFFFF.toInt() }
        val blueLinePaint = Paint().apply {
            color = 0xFFD0E4F5.toInt()
            strokeWidth = 1f
        }
        val redLinePaint = Paint().apply {
            color = 0xFFFFB2B2.toInt()
            strokeWidth = 1.5f
        }
        val textPaint = Paint().apply {
            color = 0xFF000000.toInt()
            textSize = 14f
            isAntiAlias = true
        }
        val pageNumPaint = Paint().apply {
            color = 0xFF888888.toInt()
            textSize = 11f
            isAntiAlias = true
        }

        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Draw horizontal lines
        val lineSpacing = 24f
        var y = 60f
        while (y < 820f) {
            canvas.drawLine(0f, y, 595f, y, blueLinePaint)
            y += lineSpacing
        }

        // Draw vertical red margin line
        canvas.drawLine(70f, 0f, 70f, 842f, redLinePaint)

        // Draw title header
        canvas.drawText(title, 82f, 40f, textPaint.apply { textSize = 15f; isFakeBoldText = true })
        canvas.drawLine(0f, 50f, 595f, 50f, blueLinePaint.apply { strokeWidth = 1.5f })

        // Draw text lines
        val contentPaint = Paint().apply {
            color = 0xFF222222.toInt()
            textSize = 12f
            isAntiAlias = true
        }
        val lines = content.split("\n")
        var textY = 74f
        for (line in lines) {
            if (textY > 800f) break
            canvas.drawText(line, 82f, textY, contentPaint)
            textY += 24f
        }

        // Draw page count bottom
        canvas.drawText("Page $pageNumber", 510f, 825f, pageNumPaint)
    }
}
