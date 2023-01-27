package com.binishmatheww.scanner.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.binishmatheww.scanner.common.PdfEditor
import com.binishmatheww.scanner.common.utils.getPageSize
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfEditorViewModel @Inject constructor(
    private val app: Application
): AndroidViewModel(app) {

    val pdfEditor by lazy{ PdfEditor() }

    var pages = mutableStateListOf<File>()

    var pageSize = getPageSize("DEFAULT (A4)")

    var editAtPosition = 0

    var splitAtPosition = 0

    var addImageAtPosition = 0


}