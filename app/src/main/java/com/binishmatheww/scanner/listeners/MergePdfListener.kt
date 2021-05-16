package com.binishmatheww.scanner.listeners

interface MergePdfListener {
    fun onPreExecute(count: Int)
    fun onProgressUpdate(progress: Int)
    fun onPostExecute(result: String, outputPath: String)
}