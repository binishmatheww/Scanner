package com.binishmatheww.scanner.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.binishmatheww.camera.CameraController
import com.binishmatheww.scanner.models.PdfFile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    app: Application
): AndroidViewModel(app) {

    var cameraController = CameraController(app)

    val images = mutableStateListOf<PdfFile>()

    override fun onCleared() {
        super.onCleared()
        cameraController.dispose(cause = "viewmodel got cleared.")
    }

}