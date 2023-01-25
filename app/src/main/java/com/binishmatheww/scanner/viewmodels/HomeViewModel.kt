package com.binishmatheww.scanner.viewmodels


import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.binishmatheww.scanner.models.PdfFile

class HomeViewModel: ViewModel() {

    val files = mutableStateListOf<PdfFile>()

}