package com.binishmatheww.scanner.views.listeners

import java.io.File

interface FilterImageListener {
    fun postExecute(filteredImage: File)
}