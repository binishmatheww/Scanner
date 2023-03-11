package com.binishmatheww.scanner.common.utils

import androidx.core.net.toUri
import com.binishmatheww.scanner.models.PdfFile
import java.io.File

fun File.toPdfFile(
    filterSeed: Float = 100f
): PdfFile {
    return PdfFile(
        uri = toUri(),
        name = name,
        filterSeed = filterSeed
    )
}