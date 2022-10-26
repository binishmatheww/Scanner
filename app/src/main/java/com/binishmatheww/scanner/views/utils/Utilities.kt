package com.binishmatheww.scanner.views.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.fragments.PdfEditorFragment
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import java.io.File
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt


const val masterPassWord = "DoWeReallyNeedPasswords"

const val TYPE_PDF = "application/pdf"
const val TYPE_JPG = "image/jpg"
const val TYPE_TXT = "text/plain"


fun createDocument(title: String, TYPE: String, launcher: ActivityResultLauncher<Intent>){
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        putExtra(Intent.EXTRA_TITLE,title)
        addCategory(Intent.CATEGORY_OPENABLE)
        type = TYPE
    }
    launcher.launch(intent)
}

fun openDirectoryPicker(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
    launcher.launch(intent)
}

fun openFilePicker(TYPE: String, launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = TYPE
    }
    launcher.launch(intent)
}

fun Fragment.openEditor(uri: Uri) {
    val bundle = Bundle()
    bundle.putString("uri", uri.toString())
    val frag = PdfEditorFragment()
    frag.arguments = bundle
    requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fContainer, frag, "pdfEditor").addToBackStack("pdfEditor").commitAllowingStateLoss()
}

fun Fragment.openEditor(file: File) {
    val bundle = Bundle()
    bundle.putString("file", file.absolutePath)
    val frag = PdfEditorFragment()
    frag.arguments = bundle
    requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fContainer, frag, "pdfEditor").addToBackStack("pdfEditor").commitAllowingStateLoss()
}

fun Fragment.pdfFilesFromStorageLocation() : ArrayList<File> {
    val files = ArrayList<File>()
    val children = storageLocation(requireContext()).listFiles()
    children?.let {
        for (child in children){
            if(child.name.endsWith(".pdf")){
                //Log.wtf("fromStorageLocation",child.name)
                files.add(child)
            }
        }
    }
    return files
}

fun storageLocation(context: Context) : File{
    val storageLocation = File(context.getExternalFilesDir(null), File.separator + context.getString(R.string.storage_location))
    if (!storageLocation.exists()) {
        storageLocation.mkdirs()
    }
    //Log.wtf("storageLocation",storageLocation.absolutePath)
    return storageLocation
}

fun temporaryLocation(context: Context) : File {
    val temporaryLocation = File(context.getExternalFilesDir(null), File.separator + context.getString(R.string.temporary_location))
    if (!temporaryLocation.exists()) {
        temporaryLocation.mkdirs()
    }
    //Log.wtf("temporaryLocation",temporaryLocation.absolutePath)
    return temporaryLocation
}

fun clearTemporaryLocation(context: Context){
    val temporaryLocation = File(context.getExternalFilesDir(null), File.separator + context.getString(R.string.temporary_location))
    deleteRecursively(temporaryLocation)
}

fun deleteRecursively(dir: File) {
    if (dir.isDirectory) {
        val children = dir.list()
        children?.let {
            for (i in children.indices) {
                val temp = File(dir, it[i])
                if (temp.isDirectory) {
                    deleteRecursively(temp)
                } else {
                    temp.delete()
                }
            }
        }
    }
    dir.delete()
}

fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
    var width = image.width
    var height = image.height
    val bitmapRatio = width.toFloat() / height.toFloat()
    if (bitmapRatio > 0) {
        width = maxSize
        height = (width / bitmapRatio).toInt()
    } else {
        height = maxSize
        width = (height * bitmapRatio).toInt()
    }
    return Bitmap.createScaledBitmap(image, width, height, true)
}

fun getPageSize(pageSize: String): Rectangle {
    var size: Rectangle = PageSize.A4
    when (pageSize) {
        "DEFAULT (A4)" -> size = PageSize.A4
        "A4 LANDSCAPE" -> size = PageSize.A4_LANDSCAPE
        "LETTER" -> size = PageSize.LETTER
        "HALF LETTER" -> size = PageSize.HALFLETTER
        "LEGAL" -> size = PageSize.LEGAL
        "EXECUTIVE" -> size = PageSize.EXECUTIVE
        "LEDGER" -> size = PageSize.LEDGER
        "TABLOID" -> size = PageSize.TABLOID
        "NOTE" -> size = PageSize.NOTE
        "POSTCARD" -> size = PageSize.POSTCARD
        "A0" -> size = PageSize.A0
        "A1" -> size = PageSize.A1
        "A2" -> size = PageSize.A2
        "A3" -> size = PageSize.A3
        "A5" -> size = PageSize.A5
        "A6" -> size = PageSize.A6
        "A7" -> size = PageSize.A7
        "A8" -> size = PageSize.A8
        "A9" -> size = PageSize.A9
        "A10" -> size = PageSize.A10
        "B0" -> size = PageSize.B0
        "B1" -> size = PageSize.B1
        "B2" -> size = PageSize.B2
        "B3" -> size = PageSize.B3
        "B4" -> size = PageSize.B4
        "B5" -> size = PageSize.B5
        "B6" -> size = PageSize.B6
        "B7" -> size = PageSize.B7
        "B8" -> size = PageSize.B8
        "B9" -> size = PageSize.B9
        "B10" -> size = PageSize.B10
    }
    return size
}

fun getContrastBrightnessFilter(contrast: Float, brightness: Float): ColorMatrixColorFilter {
    val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness, 0f, contrast, 0f, 0f, brightness, 0f, 0f, contrast, 0f, brightness, 0f, 0f, 0f, 1f, brightness
    ))
    return ColorMatrixColorFilter(cm)
}

fun vibrate(context: Context){
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if(vibrator.hasVibrator()){
        if(Build.VERSION.SDK_INT >= 26){
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        else{
            vibrator.vibrate(150)
        }
    }
}

fun calculateMegaPixels(size : Camera.Size) : String {
    val product = size.height.toFloat().times(size.width.toFloat())
    val mp = (product/1000000).toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    var s = "$mp Mp"
    if(mp>=2){
        s = "${mp.roundToInt()} Mp"
    }
    return s
}

fun getOptimalPreviewSize(sizes: List<Camera.Size>, w: Int, h: Int): Camera.Size? {
    val tolerance = 0.05
    val targetRatio = w.toDouble() / h
    var optimalSize: Camera.Size? = null
    var minDiff = Double.MAX_VALUE
    for (size in sizes) {
        val ratio = size.width.toDouble() / size.height
        if (abs(ratio - targetRatio) > tolerance) {
            continue
        }
        if (abs(size.height - h) < minDiff) {
            optimalSize = size
            minDiff = abs(size.height - h).toDouble()
        }
    }
    if (optimalSize == null) {
        minDiff = Double.MAX_VALUE
        for (size in sizes) {
            if (abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - h).toDouble()
            }
        }
    }
    return optimalSize
}

