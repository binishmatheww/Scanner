package com.binishmatheww.scanner.common

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.utils.ExifReader
import com.binishmatheww.scanner.views.listeners.*
import com.binishmatheww.scanner.common.utils.TYPE_JPG
import com.binishmatheww.scanner.common.utils.temporaryLocation
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.*
import com.itextpdf.text.pdf.parser.PdfImageObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.math.abs

class PdfEditor {

    suspend fun convertImageToPdf(
        imageToBeConverted: ByteArray,
        outputFile: File,
        position: Int,
        pageSize: Rectangle,
        imageToPdfListener: ImageToPdfListener
    ) = withContext(Dispatchers.IO){

        try {
            val document = Document(pageSize)
            document.setMargins(10f, 10f, 10f, 10f)
            PdfWriter.getInstance(document, FileOutputStream(outputFile))
            document.open()
            val image = Image.getInstance(imageToBeConverted)
            image.setRotationDegrees(360-ExifReader.getOrientation(imageToBeConverted))
            val documentWidth = document.pageSize.width
            val documentHeight = document.pageSize.height
            image.compressionLevel = 9
            image.scaleToFit(documentWidth, documentHeight)
            image.setAbsolutePosition(
                (document.pageSize.width - image.scaledWidth) / 2,
                (document.pageSize.height - image.scaledHeight) / 2)
            document.add(image)
            document.close()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        withContext(Dispatchers.Main){
            imageToPdfListener.postExecute(outputFile, position)
        }

    }

    suspend fun filterImage(
        filteredImage: File,
        key: Float,
        inputPage: File,
        filterImageListener: FilterImageListener
    ) = withContext(Dispatchers.IO){

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

    suspend fun compressPdf (
        pages: List<File>,
        outputFile: File,
        compressionListener: CompressionListener
    ) = withContext(Dispatchers.IO) {
        try {

            var n: Int

            val document = Document()

            val copy = PdfSmartCopy(document, FileOutputStream(outputFile))
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
                compressionListener.postExecute("Could not compress ${outputFile.name}")
            }
        }
        withContext(Dispatchers.Main){
            compressionListener.postExecute("Compressed ${outputFile.name}")
        }


    }

    suspend fun encryptPdf(
        pages : List<File>,
        outputFile : File,
        inputPassword : String,
        masterPassword : String,
        encryptPdfListener: EncryptPdfListener
    ) = withContext(Dispatchers.IO){

        withContext(Dispatchers.Main){ encryptPdfListener.onPreExecute(pages.size) }

        try {
            val document = Document()
            val copy = PdfSmartCopy(document, FileOutputStream(outputFile))
            copy.setFullCompression()
            copy.setEncryption(inputPassword.toByteArray(), masterPassword.toByteArray(), PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY or PdfWriter.ALLOW_MODIFY_CONTENTS, PdfWriter.ENCRYPTION_AES_128)
            document.open()
            for (i in pages.indices) {
                val reader = PdfReader(pages[i].absolutePath)
                copy.addDocument(reader)
                reader.close()
                withContext(Dispatchers.Main){
                    encryptPdfListener.onProgressUpdate(i+1)
                }
            }
            copy.close()
            document.close()
            withContext(Dispatchers.Main) {
                encryptPdfListener.onPostExecute("Encrypted pdf  ${outputFile.name}")
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                encryptPdfListener.onPostExecute("Couldn't encrypt pdf ${outputFile.name}")
            }
        }

    }

    suspend fun mergePdf(
        pages: List<File>,
        outputFile: File,
        mergePdfListener: MergePdfListener
    ) = withContext(Dispatchers.IO){

        withContext(Dispatchers.Main){ mergePdfListener.onPreExecute(pages.size) }

        try {
            val document = Document()
            val copy = PdfSmartCopy(document, FileOutputStream(outputFile))
            document.open()
            for (i in pages.indices) {
                val reader = PdfReader(pages[i].absolutePath)
                copy.addDocument(reader)
                reader.close()
                withContext(Dispatchers.Main){
                    mergePdfListener.onProgressUpdate(i+1)
                }
            }
            copy.close()
            copy.setFullCompression()
            document.close()
            withContext(Dispatchers.Main) {
                mergePdfListener.onPostExecute("Exported pdf ${outputFile.name}", outputFile.absolutePath)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                mergePdfListener.onPostExecute("Couldn't export pdf ${outputFile.name}", outputFile.absolutePath)
            }
        }

    }

    suspend fun extractPdfPages(
        context: Context,
        sourcePdfPath: String,
        extractionListener: PdfPageExtractorListener
    ){

        val reader = PdfReader(sourcePdfPath)
        val number  = reader.numberOfPages
        extractionListener.preExecute(number)
        extract(context,number,reader,extractionListener)

    }

    suspend fun extractPdfPages(
        context: Context,
        bytes : ByteArray,
        extractionListener: PdfPageExtractorListener
    ){

        val reader = PdfReader(bytes)
        val number  = reader.numberOfPages
        extractionListener.preExecute(number)
        extract(context,number,reader,extractionListener)

    }

    private suspend fun extract(
        context: Context,
        number : Int,
        reader : PdfReader,
        extractionListener: PdfPageExtractorListener
    ) = withContext(Dispatchers.IO){

        val pages = ArrayList<File>()

        try {

            for (i in 1..number) {
                val document = Document(reader.getPageSizeWithRotation(i))
                val pageFile = File(
                    context.temporaryLocation(),
                    context.getString(R.string.page_prefix) +
                            i +
                            "_" +
                            System.currentTimeMillis() +
                            context.getString(R.string.page_extension)
                )
                Log.wtf("page",pageFile.absolutePath)
                val writer = PdfCopy(document, FileOutputStream(pageFile))
                document.open()
                val page = writer.getImportedPage(reader, i)
                writer.addPage(page)
                document.close()
                writer.close()
                pages.add(pageFile)
                withContext(Dispatchers.Main){
                    extractionListener.progressUpdate(i+1)
                }
            }
            reader.close()

        }
        catch (e: Exception) {
            Log.wtf("ppe",e.message)
        }
        withContext(Dispatchers.Main){
            extractionListener.completed(pages)
        }

    }

    suspend fun convertPdfToImage(
        context: Context,
        name : String,
        outputDir : DocumentFile,
        pages: List<File>,
        listener: PdfToImageListener
    ) = withContext(Dispatchers.IO){

        listener.preExecute(pages.size)

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
                    withContext(Dispatchers.Main){
                        listener.progressUpdate(i)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    listener.postExecute("Failed converting  these pages into images")
                }
            }
        }

        withContext(Dispatchers.Main){
            listener.postExecute("Successfully converted these pages into images")
        }

    }

    suspend fun cropImage(
        bitmap: Bitmap,
        perspectiveListener: PerspectiveCorrectionListener
    ) = withContext(Dispatchers.IO){

        val imageWidth = bitmap.width
        val imageHeight = bitmap.height
        var topLeftX: Float
        var topLeftY: Float
        var topRightX: Float
        var topRightY: Float
        var bottomLeftX: Float
        var bottomLeftY: Float
        var bottomRightX: Float
        var bottomRightY: Float
        topLeftX = 0.0f
        topLeftY = 0.0f
        topRightX = imageWidth.toFloat()
        topRightY = 0.0f
        bottomLeftX = 0.0f
        bottomLeftY = imageHeight.toFloat()
        bottomRightX = imageWidth.toFloat()
        bottomRightY = imageHeight.toFloat()
        var src = floatArrayOf(
            topLeftX, topLeftY,
            bottomLeftX, bottomLeftY,
            bottomRightX, bottomRightY,
            topRightX, topRightY
        )
        try {
            val imageToBeProcessed = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val min = 120
            val max = 250
            var pixel: Int
            var pixelR: Int
            var pixelG: Int
            var pixelB: Int
            for (y in imageHeight / 2 downTo 1) {
                for (x in imageWidth / 2 downTo 1) {
                    pixel = imageToBeProcessed.getPixel(x, y)
                    pixelR = Color.red(pixel)
                    pixelG = Color.green(pixel)
                    pixelB = Color.blue(pixel)
                    if (!this.isActive) {
                        break
                    } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                            pixelB - pixelR
                        ) <= 10
                    ) {
                        topLeftX = x.toFloat()
                        topLeftY = y.toFloat()
                        break
                    }
                }
            }
            for (y in imageHeight / 2 downTo 1) {
                for (x in imageWidth / 2 until imageWidth) {
                    pixel = imageToBeProcessed.getPixel(x, y)
                    pixelR = Color.red(pixel)
                    pixelG = Color.green(pixel)
                    pixelB = Color.blue(pixel)
                    if (!this.isActive) {
                        break
                    } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                            pixelB - pixelR
                        ) <= 10
                    ) {
                        topRightX = x.toFloat()
                        topRightY = y.toFloat()
                        break
                    }
                }
            }
            for (x in imageWidth / 2 downTo 1) {
                for (y in imageHeight / 2 until imageHeight) {
                    pixel = imageToBeProcessed.getPixel(x, y)
                    pixelR = Color.red(pixel)
                    pixelG = Color.green(pixel)
                    pixelB = Color.blue(pixel)
                    if (!this.isActive) {
                        break
                    } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                            pixelB - pixelR
                        ) <= 10
                    ) {
                        bottomLeftX = x.toFloat()
                        bottomLeftY = y.toFloat()
                        break
                    }
                }
            }
            for (x in imageWidth / 2 until imageWidth) {
                for (y in imageHeight / 2 until imageHeight) {
                    pixel = imageToBeProcessed.getPixel(x, y)
                    pixelR = Color.red(pixel)
                    pixelG = Color.green(pixel)
                    pixelB = Color.blue(pixel)
                    if (!this.isActive) {
                        break
                    } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                            pixelB - pixelR
                        ) <= 10
                    ) {
                        bottomRightX = x.toFloat()
                        bottomRightY = y.toFloat()
                        break
                    }
                }
            }
            src = floatArrayOf(
                topLeftX, topLeftY,
                bottomLeftX, bottomLeftY,
                bottomRightX, bottomRightY,
                topRightX, topRightY
            )

            withContext(Dispatchers.Main){
                perspectiveListener.onPostExecute(src)
            }

        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                perspectiveListener.onPostExecute(src)
            }
        }

    }

    suspend fun rotatePdf(
        page: File,
        position: Int,
        rotation: Int,
        outputFile: File,
        rotatePageListener: RotatePageListener
    ) = withContext(Dispatchers.IO){

        try {
            val reader = PdfReader(page.absolutePath)
            val n = reader.numberOfPages
            var pageDict: PdfDictionary
            for (i in 1..n) {
                pageDict = reader.getPageN(i)
                pageDict.put(PdfName.ROTATE, PdfNumber(reader.getPageRotation(i) + rotation))
            }
            val stamper = PdfStamper(reader, FileOutputStream(outputFile))
            stamper.close()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        withContext(Dispatchers.Main){
            rotatePageListener.postExecute(position, outputFile)
        }

    }

}