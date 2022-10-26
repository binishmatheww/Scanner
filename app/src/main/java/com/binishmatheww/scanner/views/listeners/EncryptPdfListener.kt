package com.binishmatheww.scanner.views.listeners

interface EncryptPdfListener {
    fun onPreExecute(count : Int)
    fun onProgressUpdate(progress : Int)
    fun onPostExecute(result : String)
}