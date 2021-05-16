package com.binishmatheww.scanner.utils

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.AsyncTask
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.Process
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.FilterImageListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class FilterImage(var context: Context, private val key: Float, private val inputPage: File, var filterImageListener: FilterImageListener)  {

    init {

        CoroutineScope(IO).launch {
            val filteredImage = File(temporaryLocation(context), context.getString(R.string.image_prefix) + System.currentTimeMillis() + "_filtered" + context.getString(R.string.image_extension))
            val bitmap: Bitmap
            try {
                val cm = ColorMatrix(
                        floatArrayOf(
                                key, 0f, 0f, 0f, 0f, 0f,
                                key, 0f, 0f, 0f, 0f, 0f,
                                key, 0f, 0f, 0f, 0f, 0f, 1f, 0f
                        )
                )
                val fd =
                        ParcelFileDescriptor.open(inputPage, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                val page = renderer.openPage(0)
                bitmap = Bitmap.createBitmap(
                        page.width * 2,
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                )
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val canvas = Canvas(bitmap)
                val paint = Paint()
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                filteredImage.createNewFile()
                val os: OutputStream = BufferedOutputStream(FileOutputStream(filteredImage))
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                os.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            withContext(Main){
                filterImageListener.postExecute(filteredImage)
            }
        }
    }

    }
