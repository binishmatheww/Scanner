package com.binishmatheww.scanner.utils

import android.content.Context
import android.os.AsyncTask
import android.os.Environment
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.RotatePageListener
import com.itextpdf.text.pdf.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class RotatePage (var context: Context,var page: File,var position: Int,var rotation: Int,var rotatePageListener: RotatePageListener) {
    init {
        CoroutineScope(IO).launch {
            val outputPath = temporaryLocation(context).absolutePath + System.currentTimeMillis() + "_rotated" + context.getString(
                    R.string.page_extension
            )
            try {
                val reader = PdfReader(page.absolutePath)
                val n = reader.numberOfPages
                var pageDict: PdfDictionary
                for (i in 1..n) {
                    pageDict = reader.getPageN(i)
                    pageDict.put(PdfName.ROTATE, PdfNumber(reader.getPageRotation(i) + rotation))
                }
                val stamper = PdfStamper(reader, FileOutputStream(outputPath))
                stamper.close()
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Main){
                rotatePageListener.postExecute(position, File(outputPath))
            }
        }

    }


}
