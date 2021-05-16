package com.binishmatheww.scanner.listeners

import java.io.File

interface FilterImageListener {
    fun postExecute(filteredImage: File)
}