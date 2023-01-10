package com.binishmatheww.scanner.common.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.pdf.PdfRenderer
import android.hardware.Camera
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IdRes
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.fragments.PdfEditorFragment
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.Serializable
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToInt


const val masterPassWord = "DoWeReallyNeedPasswords"

const val TYPE_PDF = "application/pdf"
const val TYPE_JPG = "image/jpg"
const val TYPE_TXT = "text/plain"


fun Any.log(
    message: String?,
    throwable: Throwable? = null
) = log(
    tag = this::class.java.simpleName,
    message = message,
    throwable = throwable
)

fun log(
    tag : String,
    message : String?,
    throwable: Throwable? = null
) = Log.wtf(
    tag,
    message.toString(),
    throwable
)

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
    requireActivity().supportFragmentManager.beginTransaction().replace(R.id.navigationController, frag, "pdfEditor").addToBackStack("pdfEditor").commitAllowingStateLoss()
}

fun Fragment.openEditor(file: File) {
    val bundle = Bundle()
    bundle.putString("file", file.absolutePath)
    val frag = PdfEditorFragment()
    frag.arguments = bundle
    requireActivity().supportFragmentManager.beginTransaction().replace(R.id.navigationController, frag, "pdfEditor").addToBackStack("pdfEditor").commitAllowingStateLoss()
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
    temporaryLocation.deleteRecursively()
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

inline fun <reified T : Serializable> Bundle.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializable(key) as? T
}

inline fun <reified T : Serializable> Intent.serializable(key: String): T? = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getSerializableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getSerializableExtra(key) as? T
}

fun <T> Fragment.getNavigationResult(key: String = "result") =
    findNavController().currentBackStackEntry?.savedStateHandle?.get<T>(key)

fun <T> Fragment.setNavigationResult(result: T, key: String = "result") {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
}

fun <T : Parcelable> NavController.handleResult(
    lifecycleOwner: LifecycleOwner,
    @IdRes currentDestinationId: Int,
    @IdRes childDestinationId: Int,
    handler: (T) -> Unit
) {
    // `getCurrentBackStackEntry` doesn't work in case of recovery from the process death when dialog is opened.
    val currentEntry = getBackStackEntry(currentDestinationId)
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            handleResultFromChild(childDestinationId, currentEntry, handler)
        }
    }
    currentEntry.lifecycle.addObserver(observer)
    lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            currentEntry.lifecycle.removeObserver(observer)
        }
    })
}

private fun <T : Parcelable> handleResultFromChild(
    @IdRes childDestinationId: Int,
    currentEntry: NavBackStackEntry,
    handler: (T) -> Unit
) {
    val expectedResultKey = resultName(childDestinationId)
    if (currentEntry.savedStateHandle.contains(expectedResultKey)) {
        val result = currentEntry.savedStateHandle.get<T>(expectedResultKey)
        handler(result!!)
        currentEntry.savedStateHandle.remove<T>(expectedResultKey)
    }
}

fun <T : Parcelable> NavController.finishWithResult(result: T) {
    val currentDestinationId = currentDestination?.id
    if (currentDestinationId != null) {
        previousBackStackEntry?.savedStateHandle?.set(resultName(currentDestinationId), result)
    }
    popBackStack()
}

private fun resultName(resultSourceId: Int) = "result-$resultSourceId"

fun LazyListState.animateScrollAndCentralizeItem(index: Int, scope: CoroutineScope) {
    scope.launch {

        this@animateScrollAndCentralizeItem
            .layoutInfo
            .visibleItemsInfo
            .firstOrNull { it.index == index } ?: this@animateScrollAndCentralizeItem.scrollToItem(index)

        val itemInfo = this@animateScrollAndCentralizeItem
            .layoutInfo
            .visibleItemsInfo
            .firstOrNull { it.index == index }

        if (itemInfo != null) {
            val center = this@animateScrollAndCentralizeItem.layoutInfo.viewportEndOffset / 2
            val childCenter = itemInfo.offset + itemInfo.size / 2
            this@animateScrollAndCentralizeItem.animateScrollBy((childCenter - center).toFloat())
        } else {
            this@animateScrollAndCentralizeItem.animateScrollToItem(index)
        }

    }
}

fun File.getPdfPreview() : Bitmap?{

    var bitmap: Bitmap? = null

    try {
        val renderer: PdfRenderer
        val fd: ParcelFileDescriptor
        val file = File(absolutePath)
        fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(fd)
        val page: PdfRenderer.Page = renderer.openPage(0)
        bitmap = Bitmap.createBitmap(page.width , page.height , Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.White.toArgb())
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        //bitmap = getResizedBitmap(bitmap, 600)
        page.close()
        renderer.close()
    }
    catch (e: Exception) {
        e.printStackTrace()
    }

    return bitmap

}