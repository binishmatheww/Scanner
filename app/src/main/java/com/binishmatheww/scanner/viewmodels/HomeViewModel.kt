package com.binishmatheww.scanner.viewmodels


import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.binishmatheww.scanner.models.PdfFile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    app: Application
) : AndroidViewModel(app) {

    val files = mutableStateListOf<PdfFile>()

}