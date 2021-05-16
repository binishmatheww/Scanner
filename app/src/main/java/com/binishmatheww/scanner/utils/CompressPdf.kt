package com.binishmatheww.scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.AsyncTask
import android.os.Environment
import android.os.Process
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.CompressionListener
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.PdfImageObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.lang.Exception

class CompressPdf (var context: Context,var pages : ArrayList<File>,outputName : String,var compressionListener: CompressionListener) {



    init {
        CoroutineScope(IO).launch {
            try {
                var n = 0
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

                withContext(Main){
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

                    withContext(Main){
                        compressionListener.progressUpdate(p)
                    }
                }
                copy.close()
                copy.setFullCompression()
                document.close()

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Main){
                    compressionListener.postExecute("Could not compress $outputName")
                }
            }
            withContext(Main){
                compressionListener.postExecute("Compressed $outputName")
            }


        }


    }


}