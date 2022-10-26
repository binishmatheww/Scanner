package com.binishmatheww.scanner.views.listeners


interface CompressionListener {
    fun preExecute(count: Int)
    fun progressUpdate(progress: Int)
    fun postExecute(result: String)
}