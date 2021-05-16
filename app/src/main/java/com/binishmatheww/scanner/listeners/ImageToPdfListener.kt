package com.binishmatheww.scanner.listeners

import java.io.File

interface ImageToPdfListener {
    fun postExecute(result: File, position: Int)
}