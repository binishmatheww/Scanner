package com.binishmatheww.scanner.listeners

interface EncryptPdfListener {
    fun onPreExecute(count : Int)
    fun onProgressUpdate(progress : Int)
    fun onPostExecute(result : String)
}