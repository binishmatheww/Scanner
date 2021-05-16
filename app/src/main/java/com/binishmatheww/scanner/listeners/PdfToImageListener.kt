package com.binishmatheww.scanner.listeners

interface PdfToImageListener {
    fun preExecute(count: Int)
    fun progressUpdate(progress: Int)
    fun postExecute(result: String)
}