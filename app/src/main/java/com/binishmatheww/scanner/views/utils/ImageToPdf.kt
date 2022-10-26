package com.binishmatheww.scanner.views.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import android.os.Process
import android.util.Log
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.listeners.ImageToPdfListener
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageToPdf(var context: Context, var imageToBeConverted: ByteArray, private val outputPath: String, var position: Int, pageSize: Rectangle, var imgToPdfListener: ImageToPdfListener) {



    init {
        CoroutineScope(IO).launch {
            try {
                val document = Document(pageSize)
                document.setMargins(10f, 10f, 10f, 10f)
                PdfWriter.getInstance(document, FileOutputStream(outputPath))
                document.open()
                val image = Image.getInstance(imageToBeConverted)
                val documentWidth = document.pageSize.width
                val documentHeight = document.pageSize.height
                image.compressionLevel = 9
                image.scaleToFit(documentWidth, documentHeight)
                image.setAbsolutePosition(
                        (document.pageSize.width - image.scaledWidth) / 2,
                        (document.pageSize.height - image.scaledHeight) / 2)
                document.add(image)
                document.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Main){
                imgToPdfListener.postExecute(File(outputPath), position)
            }
        }

    }


}
