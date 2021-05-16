package com.binishmatheww.scanner.listeners

import java.io.File

interface RotatePageListener {
    fun postExecute(position: Int, rotatedPage : File)
}