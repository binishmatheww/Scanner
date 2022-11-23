package com.binishmatheww.scanner.common

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.listeners.CompressionListener
import com.binishmatheww.scanner.views.listeners.FilterImageListener
import com.binishmatheww.scanner.views.utils.storageLocation
import com.binishmatheww.scanner.views.utils.temporaryLocation
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.PdfImageObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

object PdfEditor {

    suspend fun filterImage(
        context: Context,
        key: Float,
        inputPage: File,
        filterImageListener: FilterImageListener
    )  {

        withContext(Dispatchers.IO){

            val filteredImage = File(
                temporaryLocation(context), context.getString(R.string.image_prefix) + System.currentTimeMillis() + "_filtered" + context.getString(
                    R.string.image_extension))
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

            withContext(Dispatchers.Main){
                filterImageListener.postExecute(filteredImage)
            }

        }

    }

    suspend fun compressPdf (
        context: Context,
        pages : ArrayList<File>,
        outputName : String,
        compressionListener: CompressionListener
    ) {


        withContext(Dispatchers.IO) {
            try {
                var n: Int
                val outputPath = storageLocation(context).absolutePath.plus(File.separator).plus(outputName).plus("_compressed").plus(context.getString(R.string.pdf_extension))
                val document = Document()

                val copy = PdfSmartCopy(document, FileOutputStream(outputPath))
                document.open()

                var obj : PdfObject?
                var stream : PRStream?
                var pdfSubType: PdfObject?
                var image : PdfImageObject?
                var imageBytes : ByteArray?
                var bmp : Bitmap?
                var scaledBitmap : Bitmap?
                var canvas : Canvas?
                var imgBytes : ByteArrayOutputStream?

                withContext(Dispatchers.Main){
                    compressionListener.preExecute(pages.size)
                }
                for (p in pages.indices) {
                    val reader = PdfReader(pages[p].absolutePath)

                    n = reader.xrefSize

                    for (i in 0 until n) {
                        try {
                            obj = reader.getPdfObject(i)
                            if (obj == null || !obj.isStream) continue
                            stream = obj as PRStream
                            pdfSubType = stream.get(PdfName.SUBTYPE)
                            if (pdfSubType.toString() == PdfName.IMAGE.toString()) {
                                image = PdfImageObject(stream)
                                imageBytes = image.imageAsBytes
                                bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: continue
                                scaledBitmap = bmp //getResizedBitmap(bmp,100)
                                canvas = Canvas()
                                canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                                imgBytes = ByteArrayOutputStream()
                                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, imgBytes)
                                stream.clear()
                                stream.setData(imgBytes.toByteArray(), false, PRStream.BEST_COMPRESSION)
                                stream.put(PdfName.TYPE, PdfName.XOBJECT)
                                stream.put(PdfName.SUBTYPE, PdfName.IMAGE)
                                stream.put(PdfName.FILTER, PdfName.DCTDECODE)
                                stream.put(PdfName.WIDTH, PdfNumber(bmp.width))
                                stream.put(PdfName.HEIGHT, PdfNumber(bmp.height))
                                stream.put(PdfName.BITSPERCOMPONENT, PdfNumber(8))
                                stream.put(PdfName.COLORSPACE, PdfName.DEVICERGB)
                            }
                        }catch (e : Exception){
                            e.printStackTrace()
                        }
                    }
                    reader.removeUnusedObjects()

                    copy.addDocument(reader)
                    reader.close()

                    withContext(Dispatchers.Main){
                        compressionListener.progressUpdate(p)
                    }
                }
                copy.close()
                copy.setFullCompression()
                document.close()

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    compressionListener.postExecute("Could not compress $outputName")
                }
            }
            withContext(Dispatchers.Main){
                compressionListener.postExecute("Compressed $outputName")
            }


        }


    }

}