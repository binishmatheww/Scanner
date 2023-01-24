package com.binishmatheww.scanner.models

import android.net.Uri
import java.io.File

data class PdfFile(
    val uri: Uri,
    val displayName: String,
    val file: File? = null,
)
