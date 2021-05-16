package com.binishmatheww.scanner.utils

import android.content.Context
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.MergePdfListener
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfSmartCopy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.collections.ArrayList

class MergePdf(context : Context, private var pages: ArrayList<File>, outputName: String, private var mergePdfListener: MergePdfListener){
    init {
        mergePdfListener.onPreExecute(pages.size)
        val outputPath = storageLocation(context).absolutePath.plus(File.separator).plus(outputName).plus(context.getString(R.string.pdf_extension))
        CoroutineScope(IO).launch {
            try {
                val document = Document()
                val copy = PdfSmartCopy(document, FileOutputStream(outputPath))
                document.open()
                for (i in pages.indices) {
                    val reader = PdfReader(pages[i].absolutePath)
                    copy.addDocument(reader)
                    reader.close()
                    withContext(Main){
                        mergePdfListener.onProgressUpdate(i+1)
                    }
                }
                copy.close()
                copy.setFullCompression()
                document.close()
                withContext(Main) {
                    mergePdfListener.onPostExecute("Exported pdf $outputName", outputPath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Main) {
                    mergePdfListener.onPostExecute("Couldn't export pdf $outputName", outputPath)
                }
            }
        }
    }


}