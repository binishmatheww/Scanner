package com.binishmatheww.scanner.views.listeners

import java.io.File

interface RotatePageListener {
    fun postExecute(position: Int, rotatedPage : File)
}