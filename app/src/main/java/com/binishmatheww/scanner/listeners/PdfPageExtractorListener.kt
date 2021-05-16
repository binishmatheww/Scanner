package com.binishmatheww.scanner.listeners

import java.io.File
import java.util.ArrayList

interface PdfPageExtractorListener {
        fun preExecute(count : Int)
        fun progressUpdate(progress : Int)
        fun completed(extractedPages: ArrayList<File>)
}