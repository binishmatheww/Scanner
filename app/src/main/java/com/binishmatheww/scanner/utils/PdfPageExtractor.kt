package com.binishmatheww.scanner.utils

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.PdfPageExtractorListener
import com.itextpdf.text.Document
import com.itextpdf.text.exceptions.BadPasswordException
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class PdfPageExtractor {

    private val pages = ArrayList<File>()

    @Throws(BadPasswordException::class)
    constructor(context: Context, sourcePdfPath: String, extractionListener: PdfPageExtractorListener){
        val reader = PdfReader(sourcePdfPath)
        val number  = reader.numberOfPages
        extractionListener.preExecute(number)
        extract(context,number,reader,extractionListener)
    }

    @Throws(BadPasswordException::class)
    constructor(context: Context, bytes : ByteArray, extractionListener: PdfPageExtractorListener)  {
        val reader = PdfReader(bytes)
        val number  = reader.numberOfPages
        extractionListener.preExecute(number)
        extract(context,number,reader,extractionListener)
    }

    private fun extract(context: Context,number : Int,reader : PdfReader,extractionListener: PdfPageExtractorListener) = CoroutineScope(IO).launch {

        try {
            for (i in 1..number) {
                val outputPdfName = context.getString(R.string.page_prefix) + i + "_" + System.currentTimeMillis() + context.getString(R.string.page_extension)
                val document = Document(reader.getPageSizeWithRotation(i))
                val pageFile = File(temporaryLocation(context), outputPdfName)
                Log.wtf("page",pageFile.absolutePath)
                val writer = PdfCopy(document, FileOutputStream(pageFile))
                document.open()
                val page = writer.getImportedPage(reader, i)
                writer.addPage(page)
                document.close()
                writer.close()
                pages.add(pageFile)
                withContext(Main){
                    extractionListener.progressUpdate(i+1)
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.wtf("ppe",e.message)
        }
        withContext(Main){
            extractionListener.completed(pages)
        }
    }

}