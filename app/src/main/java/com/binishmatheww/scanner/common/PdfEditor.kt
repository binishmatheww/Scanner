package com.binishmatheww.scanner.common

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.binishmatheww.scanner.common.utils.ExifReader
import com.binishmatheww.scanner.common.utils.TYPE_JPG
import com.binishmatheww.scanner.common.utils.temporaryLocation
import com.binishmatheww.scanner.common.utils.toPdfFile
import com.binishmatheww.scanner.models.PdfFile
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

    object Constants {

        const val SCANNER_PREFERENCES = "SCANNER_PREFERENCES"
        const val PAGE_SIZE = "PAGE_SIZE"
        const val PAGE_SIZE_INDEX = "PAGE_SIZE_INDEX"
        const val CAMERA_SIZE_INDEX = "CAMERA_SIZE_INDEX"
        const val PDF_PREFIX ="PDF_"
        const val PAGE_PREFIX = "PAGE-"
        const val IMAGE_PREFIX = "IMAGE_"
        const val PDF_EXTENSION = ".pdf"
        const val PAGE_EXTENSION = ".page"
        const val IMAGE_EXTENSION = ".image"
        const val JPG_EXTENSION = ".jpg"
        const val PNG_EXTENSION = ".png"
        const val OUTPUT_FOLDER = "Output"
        const val TEMP_FOLDER = "temp"
        const val CACHE_FOLDER = "cache"

    }

    suspend fun convertImageToPdf(
        imageToBeConverted: ByteArray,
        outputFile: File,
        position: Int,
        pageSize: Rectangle,
        onPostExecute: (Boolean, Int, PdfFile?) -> Unit,
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
            withContext(Dispatchers.Main){
                onPostExecute(true, position, outputFile.toPdfFile())
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                onPostExecute(false, position, null)
            }
        }
    }

    suspend fun filterImage(
        context: Context,
        inputPage: PdfFile,
        filteredImage: File,
        key: Float,
        onPostExecute: (Boolean, PdfFile?) -> Unit
    ) = withContext(Dispatchers.IO){
        try {
            val bitmap: Bitmap
            val cm = ColorMatrix(
                floatArrayOf(
                    key, 0f, 0f, 0f, 0f, 0f,
                    key, 0f, 0f, 0f, 0f, 0f,
                    key, 0f, 0f, 0f, 0f, 0f, 1f, 0f
                )
            )
            context.contentResolver.openFileDescriptor(inputPage.uri, "r")?.use { parcelFileDescriptor ->
                val renderer = PdfRenderer(parcelFileDescriptor)
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
                withContext(Dispatchers.Main){
                    onPostExecute.invoke(true, filteredImage.toPdfFile())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                onPostExecute.invoke(false, null)
            }
        }
    }

    suspend fun compressPdf (
        context: Context,
        pages: List<PdfFile>,
        outputFile: File,
        onPreExecute: (Int) -> Unit,
        onProgressUpdate: (Int) -> Unit,
        onPostExecute: (Boolean) -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            val document = Document()
            val copy = PdfSmartCopy(document, FileOutputStream(outputFile))
            document.open()
            withContext(Dispatchers.Main){
                onPreExecute.invoke(pages.size)
            }
            pages.forEachIndexed { index, pdfFile ->
                context.contentResolver.openInputStream(pdfFile.uri)?.let { inputStream ->
                    var obj : PdfObject?
                    var stream : PRStream?
                    var pdfSubType: PdfObject?
                    var image : PdfImageObject?
                    var imageBytes : ByteArray?
                    var bmp : Bitmap?
                    var scaledBitmap : Bitmap?
                    var canvas : Canvas?
                    var imgBytes : ByteArrayOutputStream?
                    val reader = PdfReader(inputStream)
                    val n = reader.xrefSize
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
                }
                withContext(Dispatchers.Main){
                    onProgressUpdate.invoke(index)
                }
            }
            copy.close()
            copy.setFullCompression()
            document.close()
            withContext(Dispatchers.Main){
                onPostExecute.invoke(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                onPostExecute.invoke(false)
            }
        }
    }

    suspend fun encryptPdf(
        context: Context,
        pages : List<PdfFile>,
        outputFile : File,
        inputPassword : String,
        masterPassword : String,
        onPreExecute: (Int) -> Unit,
        onProgressUpdate: (Int) -> Unit,
        onPostExecute: (Boolean) -> Unit,
    ) = withContext(Dispatchers.IO){
        withContext(Dispatchers.Main){
            onPreExecute.invoke(pages.size)
        }
        try {
            val document = Document()
            val copy = PdfSmartCopy(document, FileOutputStream(outputFile))
            copy.setFullCompression()
            copy.setEncryption(inputPassword.toByteArray(), masterPassword.toByteArray(), PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY or PdfWriter.ALLOW_MODIFY_CONTENTS, PdfWriter.ENCRYPTION_AES_128)
            document.open()
            pages.forEachIndexed { index, pdfFile ->
                context.contentResolver.openInputStream(pdfFile.uri)?.use{ inputStream ->
                    val reader = PdfReader(inputStream)
                    copy.addDocument(reader)
                    reader.close()
                    withContext(Dispatchers.Main){
                        onProgressUpdate.invoke(index+1)
                    }
                }
            }
            copy.close()
            document.close()
            withContext(Dispatchers.Main) {
                onPostExecute.invoke(true)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onPostExecute.invoke(true)
            }
        }
    }

    suspend fun mergePdf(
        context: Context,
        pages: List<PdfFile>,
        outputFile: File,
        onPreExecute: (Int) -> Unit,
        onProgressUpdate: (Int) -> Unit,
        onPostExecute: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO){
        withContext(Dispatchers.Main){
            onPreExecute(pages.size)
        }
        try {
            val document = Document()
            val copy = PdfSmartCopy(document, FileOutputStream(outputFile))
            document.open()
            pages.forEachIndexed { index, pdfFile ->
                context.contentResolver.openInputStream(pdfFile.uri)?.use { inputStream ->
                    val reader = PdfReader(inputStream)
                    copy.addDocument(reader)
                    reader.close()
                    withContext(Dispatchers.Main){
                        onProgressUpdate(index+1)
                    }
                }
            }
            copy.close()
            copy.setFullCompression()
            document.close()
            withContext(Dispatchers.Main) {
                onPostExecute(true)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onPostExecute(false)
            }
        }
    }

    suspend fun extractPdfPages(
        context: Context,
        sourcePdfPath: String,
        onPreExecute: (Int) -> Unit,
        onProgressUpdate: (Int) -> Unit,
        onPostExecute: (Boolean, List<PdfFile>) -> Unit,
    ) = extract(
        context = context,
        reader = PdfReader(sourcePdfPath),
        onPreExecute = onPreExecute,
        onProgressUpdate = onProgressUpdate,
        onPostExecute = onPostExecute
    )

    suspend fun extractPdfPages(
        context: Context,
        bytes : ByteArray,
        onPreExecute: (Int) -> Unit,
        onProgressUpdate: (Int) -> Unit,
        onPostExecute: (Boolean, List<PdfFile>) -> Unit,
    ) = extract(
        context = context,
        reader = PdfReader(bytes),
        onPreExecute = onPreExecute,
        onProgressUpdate = onProgressUpdate,
        onPostExecute = onPostExecute
    )

    private suspend fun extract(
        context: Context,
        reader : PdfReader,
        onPreExecute: (Int) -> Unit,
        onProgressUpdate: (Int) -> Unit,
        onPostExecute: (Boolean, List<PdfFile>) -> Unit,
    ) = withContext(Dispatchers.IO){
        val pages = ArrayList<PdfFile>()
        try {
            val number = reader.numberOfPages
            withContext(Dispatchers.Main){
                onPreExecute.invoke(number)
            }
            for (i in 1..number) {
                val document = Document(reader.getPageSizeWithRotation(i))
                val pageFile = File(
                    context.temporaryLocation(),
                    Constants.PDF_PREFIX +
                            i +
                            "_" +
                            System.currentTimeMillis() +
                            Constants.PDF_EXTENSION
                )
                Log.wtf("page",pageFile.absolutePath)
                val writer = PdfCopy(document, FileOutputStream(pageFile))
                document.open()
                val page = writer.getImportedPage(reader, i)
                writer.addPage(page)
                document.close()
                writer.close()
                pages.add(pageFile.toPdfFile())
                withContext(Dispatchers.Main){
                    onProgressUpdate.invoke(i+1)
                }
            }
            reader.close()
            withContext(Dispatchers.Main){
                onPostExecute.invoke(true, pages)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                onPostExecute.invoke(false, emptyList())
            }
        }
    }

    suspend fun convertPdfToImage(
        context: Context,
        name : String,
        outputDir : DocumentFile,
        pages: List<PdfFile>,
        onPreExecute: (Int) -> Unit,
        onProgressUpdate: (Int) -> Unit,
        onPostExecute: (Boolean, List<Uri>) -> Unit,
    ) = withContext(Dispatchers.IO){
        withContext(Dispatchers.Main){
            onPreExecute.invoke(pages.size)
        }
        val images = arrayListOf<Uri>()
        if (pages.isNotEmpty()) {
            val root = outputDir.createDirectory(name)
            try {
                pages.forEachIndexed { index, pdfFile ->
                    context.contentResolver.openFileDescriptor(pdfFile.uri,"r")?.use { parcelFileDescriptor ->
                        val renderer = PdfRenderer(parcelFileDescriptor)
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
                            val imageFile = root?.createFile(TYPE_JPG,Constants.PAGE_PREFIX + (index + 1) + Constants.JPG_EXTENSION) ?: return@use
                            val os: OutputStream = BufferedOutputStream(context.contentResolver.openOutputStream(imageFile.uri))
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                            os.close()
                            images.add(imageFile.uri)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: SecurityException) {
                            e.printStackTrace()
                        }
                        page.close()
                        withContext(Dispatchers.Main){
                            onProgressUpdate.invoke(index)
                        }
                    }
                }
                withContext(Dispatchers.Main){
                    onPostExecute.invoke(true, images)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main){
                    onPostExecute.invoke(false, emptyList())
                }
            }
        }
    }

    suspend fun cropImage(
        bitmap: Bitmap,
        onPostExecute: (Boolean, FloatArray) -> Unit,
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
                    } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(pixelB - pixelR) <= 10) {
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
                onPostExecute.invoke(true, src)
            }
        }
        catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                onPostExecute.invoke(false, src)
            }
        }
    }

    suspend fun rotatePdf(
        context: Context,
        page: PdfFile,
        position: Int,
        rotation: Int,
        outputFile: File,
        onPostExecute: (Boolean, Int, PdfFile?) -> Unit,
    ) = withContext(Dispatchers.IO){
        try {
            context.contentResolver.openInputStream(page.uri)?.use { inputStream ->
                val reader = PdfReader(inputStream)
                val n = reader.numberOfPages
                var pageDict: PdfDictionary
                for (i in 1..n) {
                    pageDict = reader.getPageN(i)
                    pageDict.put(PdfName.ROTATE, PdfNumber(reader.getPageRotation(i) + rotation))
                }
                val stamper = PdfStamper(reader, FileOutputStream(outputFile))
                stamper.close()
                reader.close()
            }
            withContext(Dispatchers.Main){
                onPostExecute.invoke(true, position, outputFile.toPdfFile())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main){
                onPostExecute.invoke(false, position, outputFile.toPdfFile())
            }
        }
    }

}