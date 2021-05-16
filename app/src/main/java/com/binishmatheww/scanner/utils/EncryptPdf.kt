package com.binishmatheww.scanner.utils

import android.content.Context
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.EncryptPdfListener
import com.itextpdf.text.Document
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfSmartCopy
import com.itextpdf.text.pdf.PdfStamper
import com.itextpdf.text.pdf.PdfWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class EncryptPdf(context : Context, pages : ArrayList<File>, outputName : String,inputPassword : String,masterPassword : String, encryptPdfListener: EncryptPdfListener) {
    init {
        encryptPdfListener.onPreExecute(pages.size)
        val outputPath = storageLocation(context).absolutePath.plus(File.separator).plus(outputName).plus("_encrypted").plus(context.getString(R.string.pdf_extension))
        CoroutineScope(IO).launch {
            try {
                val document = Document()
                val copy = PdfSmartCopy(document, FileOutputStream(outputPath))
                copy.setFullCompression()
                copy.setEncryption(inputPassword.toByteArray(), masterPassword.toByteArray(), PdfWriter.ALLOW_PRINTING or PdfWriter.ALLOW_COPY or PdfWriter.ALLOW_MODIFY_CONTENTS, PdfWriter.ENCRYPTION_AES_128)
                document.open()
                for (i in pages.indices) {
                    val reader = PdfReader(pages[i].absolutePath)
                    copy.addDocument(reader)
                    reader.close()
                    withContext(Main){
                        encryptPdfListener.onProgressUpdate(i+1)
                    }
                }
                copy.close()
                document.close()
                withContext(Main) {
                    encryptPdfListener.onPostExecute("Encrypted pdf  $outputName")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Main) {
                    encryptPdfListener.onPostExecute("Couldn't encrypt pdf $outputName")
                }
            }
        }
    }
}