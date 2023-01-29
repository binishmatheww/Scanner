package com.binishmatheww.scanner.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.binishmatheww.scanner.common.PdfEditor
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfEditorViewModel @Inject constructor(
    app: Application
): AndroidViewModel(app) {

    val pdfEditor by lazy{ PdfEditor() }

    var pages = mutableStateListOf<File>()

    var pageSize: Rectangle = PageSize.A4

    var editAtPosition = 0

    var splitAtPosition = 0

    var addImageAtPosition = 0

}