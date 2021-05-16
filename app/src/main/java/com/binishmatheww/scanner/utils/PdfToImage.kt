package com.binishmatheww.scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.PdfToImageListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.collections.ArrayList

class PdfToImage(var context: Context,private val name : String,private val outputDir : DocumentFile, private val pages: ArrayList<File>, var listener: PdfToImageListener)  {
    init {
        listener.preExecute(pages.size)
        CoroutineScope(IO).launch {
            if (pages.isNotEmpty()) {
                val root = outputDir.createDirectory(name)
                try {
                    for (i in pages.indices) {
                        val file = File(pages[i].absolutePath)
                        val renderer = PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
                        val page = renderer.openPage(0)
                        val bitmap = Bitmap.createBitmap(
                                page.width * 2,
                                page.height * 2,
                                Bitmap.Config.ARGB_8888
                        )
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        canvas.drawBitmap(bitmap, 0f, 0f, null)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        try {
                            val image = root?.createFile(TYPE_JPG,context.getString(R.string.page_prefix) + (i + 1) + context.getString(R.string.jpeg_extension))
                            val os: OutputStream = BufferedOutputStream(context.contentResolver.openOutputStream(image!!.uri))
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                            os.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }
                        page.close()
                        withContext(Main){
                            listener.progressUpdate(i)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Main){
                        listener.postExecute("Failed converting  these pages into images")
                    }
                }
            }

            withContext(Main){
                listener.postExecute("Successfully converted these pages into images")
            }
        }
    }

}
