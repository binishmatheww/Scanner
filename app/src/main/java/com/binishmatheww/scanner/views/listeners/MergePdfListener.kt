package com.binishmatheww.scanner.views.listeners

interface MergePdfListener {
    fun onPreExecute(count: Int)
    fun onProgressUpdate(progress: Int)
    fun onPostExecute(result: String, outputPath: String)
}