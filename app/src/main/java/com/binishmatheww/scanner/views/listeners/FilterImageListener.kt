package com.binishmatheww.scanner.views.listeners

import java.io.File

interface FilterImageListener {
    suspend fun postExecute(filteredImage: File)
}