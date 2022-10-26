package com.binishmatheww.scanner.views.listeners

import java.io.File

interface ImageToPdfListener {
    fun postExecute(result: File, position: Int)
}