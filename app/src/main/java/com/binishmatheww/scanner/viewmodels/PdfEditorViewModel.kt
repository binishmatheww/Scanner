package com.binishmatheww.scanner.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.PdfEditor
import com.binishmatheww.scanner.common.utils.masterPassWord
import com.binishmatheww.scanner.common.utils.storageLocation
import com.binishmatheww.scanner.models.PdfFile
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import com.itextpdf.text.exceptions.BadPasswordException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PdfEditorViewModel @Inject constructor(
    private val app: Application
): AndroidViewModel(app) {

    val pdfEditor by lazy{ PdfEditor() }

    private var currentJob: Job? = null

    private val calendar = Calendar.getInstance()

    private var timeStamp = PdfEditor.Constants.PDF_PREFIX
        .plus(calendar.get(Calendar.YEAR))
        .plus("-")
        .plus((calendar.get(Calendar.MONTH) + 1))
        .plus("-")
        .plus(calendar.get(Calendar.DAY_OF_MONTH))
        .plus("_")
        .plus(calendar.get(Calendar.HOUR))
        .plus(":")
        .plus(calendar.get(Calendar.MINUTE))
        .plus(":")
        .plus(calendar.get(Calendar.SECOND))

    var pages = mutableStateListOf<PdfFile>()

    var shouldShowEditorExtraDialog = mutableStateOf(false)
    set(value) {
        shouldShowProgressBarDialog.value = false
        shouldShowEncryptPdfDialog.value = false
        shouldShowSplitPdfDialog.value = false
        field = value
    }

    var shouldShowProgressBarDialog = mutableStateOf(false)
        set(value) {
            shouldShowEditorExtraDialog.value = false
            shouldShowEncryptPdfDialog.value = false
            shouldShowSplitPdfDialog.value = false
            field = value
        }

    var shouldShowEncryptPdfDialog = mutableStateOf(false)
        set(value) {
            shouldShowEditorExtraDialog.value = false
            shouldShowProgressBarDialog.value = false
            shouldShowSplitPdfDialog.value = false
            field = value
        }

    var shouldShowSplitPdfDialog = mutableStateOf(false)
        set(value) {
            shouldShowEditorExtraDialog.value = false
            shouldShowProgressBarDialog.value = false
            shouldShowEncryptPdfDialog.value = false
            field = value
        }

    var progressBarDialogTitle = mutableStateOf("")

    var progress = mutableStateOf(0f)

    var password = mutableStateOf("")

    var pageSize: Rectangle = PageSize.A4

    var editAtPosition = 0

    var splitAtPosition = mutableStateOf(0)

    var addImageAtPosition = 0

    fun cancelCurrentJob(){
        currentJob?.cancel()
        currentJob = null
    }

    fun renderPages(
        inputPath: String
    ){
        currentJob?.cancel()
        currentJob = null
        currentJob = viewModelScope.launch {
            try {
                pdfEditor.extractPdfPages(
                    context = app,
                    sourcePdfPath = inputPath,
                    onPreExecute = {
                        progressBarDialogTitle.value = app.getString(R.string.Rendering)
                        shouldShowProgressBarDialog.value = true
                    },
                    onProgressUpdate = { progress, count ->
                        this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                    },
                    onPostExecute = { isSuccessful, pdfFiles ->
                        shouldShowProgressBarDialog.value = false
                        progress.value = 0f
                        if(isSuccessful){
                            if (pdfFiles.isNotEmpty()) {
                                if (pages.isNotEmpty()) {
                                    var i = pages.size
                                    var j = 0
                                    while (j < pdfFiles.size) {
                                        pages.add(i, pdfFiles[j])
                                        i++
                                        j++
                                    }
                                } else {
                                    pages.clear()
                                    pages.addAll(pdfFiles)
                                }
                            }
                        }
                    }
                )
            }
            catch (e : BadPasswordException){
                progress.value = 0f
            //TODO propagate error message
            }
        }
    }

    fun renderPages(
        inputBytes: ByteArray
    ){
        currentJob?.cancel()
        currentJob = null
        currentJob = viewModelScope.launch {
            try {
                pdfEditor.extractPdfPages(
                    context = app,
                    bytes = inputBytes,
                    onPreExecute = {
                        progressBarDialogTitle.value = app.getString(R.string.Rendering)
                        shouldShowProgressBarDialog.value = true
                    },
                    onProgressUpdate = { progress, count ->
                        this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                    },
                    onPostExecute = { isSuccessful, pdfFiles ->
                        shouldShowProgressBarDialog.value = false
                        progress.value = 0f
                        if(isSuccessful){
                            if (pdfFiles.isNotEmpty()) {
                                if (pages.isNotEmpty()) {
                                    var i = pages.size
                                    var j = 0
                                    while (j < pdfFiles.size) {
                                        pages.add(i, pdfFiles[j])
                                        i++
                                        j++
                                    }
                                } else {
                                    pages.clear()
                                    pages.addAll(pdfFiles)
                                }
                            }
                        }
                    }
                )
            }
            catch (e : BadPasswordException){
                progress.value = 0f
                //TODO propagate error message
            }

        }
    }

    fun convertPdfToImage(
        outputDirUri: Uri,
        pages: List<PdfFile> = this.pages
    ){
        currentJob?.cancel()
        currentJob = null
        currentJob = viewModelScope.launch {
            val outputDir = DocumentFile.fromTreeUri(app, outputDirUri) ?: return@launch //TODO proper handling
            pdfEditor.convertPdfToImage(
                context = app,
                name = timeStamp,
                outputDir = outputDir,
                pages = pages.toList(),
                onPreExecute = {
                    progressBarDialogTitle.value = app.getString(R.string.ConvertingPdfToImages)
                    shouldShowProgressBarDialog.value = true
                },
                onProgressUpdate = { progress, count ->
                    this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                },
                onPostExecute = onPostExecute@ { isSuccessful, _ ->
                    shouldShowProgressBarDialog.value = false
                    progress.value = 0f
                    //TODO
                },
            )
        }
    }

    fun exportPdf(
        pages: List<PdfFile> = this.pages
    ){
        currentJob?.cancel()
        currentJob = null
        currentJob = viewModelScope.launch {

            pdfEditor.mergePdf(
                context = app,
                pages = pages,
                outputFile = File(app.storageLocation().absolutePath.plus(File.separator).plus(timeStamp).plus(PdfEditor.Constants.PDF_EXTENSION)),
                onPreExecute = {
                    progressBarDialogTitle.value = app.getString(R.string.Exporting)
                    shouldShowProgressBarDialog.value = true
                },
                onProgressUpdate = { progress, count ->
                    this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                },
                onPostExecute = onPostExecute@ { isSuccessful ->
                    shouldShowProgressBarDialog.value = false
                    progress.value = 0f
                    //TODO
                },
            )

        }
    }

    fun compressPdf(
        pages: List<PdfFile> = this.pages
    ){
        currentJob?.cancel()
        currentJob = null
        currentJob = viewModelScope.launch {
            pdfEditor.compressPdf(
                context = app,
                pages = pages.toList(),
                outputFile = File(
                    app.storageLocation().absolutePath
                        .plus(File.separator)
                        .plus(timeStamp)
                        .plus("_compressed")
                        .plus(PdfEditor.Constants.PDF_EXTENSION)
                ),
                onPreExecute = {
                    progressBarDialogTitle.value = app.getString(R.string.Compressing)
                    shouldShowProgressBarDialog.value = true
                },
                onProgressUpdate = { progress, count ->
                    this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                },
                onPostExecute = onPostExecute@ { isSuccessful ->
                    shouldShowProgressBarDialog.value = false
                    progress.value = 0f
                    //TODO
                },
            )
        }
    }

    fun encryptPdf(
        pages: List<PdfFile> = this.pages,
        inputPassword: String
    ){
        currentJob?.cancel()
        currentJob = null
        currentJob = viewModelScope.launch {
            if (inputPassword.isNotEmpty()) {
                pdfEditor.encryptPdf(
                    context = app,
                    pages = pages.toList(),
                    outputFile = File(
                        app.storageLocation().absolutePath
                            .plus(File.separator)
                            .plus(timeStamp)
                            .plus("_encrypted")
                            .plus(PdfEditor.Constants.PDF_EXTENSION)
                    ),
                    inputPassword = inputPassword,
                    masterPassword = masterPassWord,
                    onPreExecute = {
                        progressBarDialogTitle.value = app.getString(R.string.Encrypting)
                        shouldShowProgressBarDialog.value = true
                    },
                    onProgressUpdate = { progress, count ->
                        this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                    },
                    onPostExecute = onPostExecute@ { isSuccessful ->
                        shouldShowProgressBarDialog.value = false
                        progress.value = 0f
                        //TODO
                    },
                )
            }
        }
    }

    fun splitPdf(
        firstPdfPages: List<PdfFile>,
        secondPdfPages: List<PdfFile>
    ){
        currentJob?.cancel()
        currentJob = null
        currentJob = viewModelScope.launch {
            //TODO rethink
            val firstJob = async {
                pdfEditor.mergePdf(
                    context = app,
                    pages = firstPdfPages,
                    outputFile = File(app.storageLocation().absolutePath.plus(File.separator).plus(timeStamp).plus(PdfEditor.Constants.PDF_EXTENSION)),
                    onPreExecute = {
                        progressBarDialogTitle.value = app.getString(R.string.Exporting)
                        shouldShowProgressBarDialog.value = true
                    },
                    onProgressUpdate = { progress, count ->
                        this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                    },
                    onPostExecute = onPostExecute@ { isSuccessful ->
                        shouldShowProgressBarDialog.value = false
                        progress.value = 0f
                        //TODO
                    },
                )
            }
            val secondJob = async {
                pdfEditor.mergePdf(
                    context = app,
                    pages = secondPdfPages,
                    outputFile = File(app.storageLocation().absolutePath.plus(File.separator).plus(timeStamp).plus(PdfEditor.Constants.PDF_EXTENSION)),
                    onPreExecute = {
                        progressBarDialogTitle.value = app.getString(R.string.Exporting)
                        shouldShowProgressBarDialog.value = true
                    },
                    onProgressUpdate = { progress, count ->
                        this@PdfEditorViewModel.progress.value = progress.toFloat()/count.toFloat()
                    },
                    onPostExecute = onPostExecute@ { isSuccessful ->
                        shouldShowProgressBarDialog.value = false
                        progress.value = 0f
                        //TODO
                    },
                )
            }
            firstJob.await()
            secondJob.await()
            shouldShowProgressBarDialog.value = false
            progress.value = 0f
        }
    }

}