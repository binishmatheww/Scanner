package com.binishmatheww.scanner.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.binishmatheww.scanner.common.PdfEditor
import com.binishmatheww.scanner.models.PdfFile
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PdfEditorViewModel @Inject constructor(
    app: Application
): AndroidViewModel(app) {

    val pdfEditor by lazy{ PdfEditor() }

    var pages = mutableStateListOf<PdfFile>()

    var shouldShowEditorExtraDialog = mutableStateOf(false)

    var pageSize: Rectangle = PageSize.A4

    var editAtPosition = 0

    var splitAtPosition = 0

    var addImageAtPosition = 0

}