package com.binishmatheww.scanner.common.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.hardware.Camera
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.IdRes
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.binishmatheww.camera.models.SmartSize
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.fragments.PdfEditorFragment
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
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

fun Fragment.pdfFilesFromStorageLocation(): ArrayList<File> {
    val files = ArrayList<File>()
    activity?.storageLocation()?.listFiles()?.let { children ->
        for (child in children){
            if(child.name.endsWith(".pdf")){
                //Log.wtf("fromStorageLocation",child.name)
                files.add(child)
            }
        }
    }
    return files
}

fun Context.getPdfFiles(): ArrayList<Uri>{

    val pdfFiles = ArrayList<Uri>()

    if(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        && !Environment.isExternalStorageManager()
    ){
            return pdfFiles
    }

    if(
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
    ){
            return pdfFiles
    }

    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.TITLE,
        MediaStore.Files.FileColumns.SIZE
    )

    val selection = MediaStore.Files.FileColumns.MIME_TYPE + " IN ('" + "application/pdf" + "')"
    val orderBy = MediaStore.Files.FileColumns.SIZE + " DESC"

    applicationContext
        .contentResolver
        .query(
            MediaStore
                .Files
                .getContentUri("external"),
            projection,
            selection,
            null,
            orderBy
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)

            log("cursor has ${cursor.count} items.")

            while ( cursor.moveToNext() ){

                pdfFiles.add(
                    Uri.withAppendedPath(
                        MediaStore.Files.getContentUri("external"),
                        cursor.getString(idColumn)
                    )
                )

            }

        }

    return pdfFiles

}

fun Context.storageLocation(): File{
    val storageLocation = File(getExternalFilesDir(null), File.separator + getString(R.string.storage_location))
    if (!storageLocation.exists()) {
        storageLocation.mkdirs()
    }
    //Log.wtf("storageLocation",storageLocation.absolutePath)
    return storageLocation
}

fun Context.temporaryLocation(): File {
    val temporaryLocation = File(getExternalFilesDir(null), File.separator + getString(R.string.temporary_location))
    if (!temporaryLocation.exists()) {
        temporaryLocation.mkdirs()
    }
    //Log.wtf("temporaryLocation",temporaryLocation.absolutePath)
    return temporaryLocation
}

fun Context.clearTemporaryLocation(){
    temporaryLocation().deleteRecursively()
}

fun Context.hasExternalStoragePermissions(): Boolean{

    return if(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    ){
        Environment.isExternalStorageManager()
    }
    else if(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    ){
        checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
    else{
        true
    }

}

fun Activity.requestExternalStoragePermissions(){

    if(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        && !Environment.isExternalStorageManager()
    ){
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            )
        )
    }
    else if(
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    ){
        requestPermissions(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            666
        )
    }

}

fun getResizedBitmap(
    image: Bitmap,
    maxSize: Int
): Bitmap {
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

fun getPageSize(
    pageSize: String
): Rectangle {
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

fun List<SmartSize>.getOptimalSizeFor(
    w: Int,
    h: Int
): SmartSize? {

    val tolerance = 0.05

    val targetRatio = w.toDouble() / h

    var optimalSize : SmartSize? = null

    var minDiff = Double.MAX_VALUE

    for (size in this) {

        val ratio = size.size.width.toDouble() / size.size.height

        if (abs(ratio - targetRatio) > tolerance) {
            continue
        }

        if (abs(size.size.height - h) < minDiff) {
            optimalSize = size
            minDiff = abs(size.size.height - h).toDouble()
        }

    }

    if (optimalSize == null) {

        minDiff = Double.MAX_VALUE

        for (size in this) {
            if (abs(size.size.height - h) < minDiff) {
                optimalSize = size
                minDiff = abs(size.size.height - h).toDouble()
            }
        }

    }

    return optimalSize ?: this.firstOrNull()
}

fun Bitmap.toFile(
    context: Context,
    fileName: String
): File? {

    var file: File? = null

    return try {
        file = File(context.storageLocation().absolutePath + File.separator + fileName)
        file.createNewFile()

        val bos = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 0, bos)
        val bitmapData = bos.toByteArray()

        val fos = FileOutputStream(file)
        fos.write(bitmapData)
        fos.flush()
        fos.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        file // it will return null
    }
}

//https://stackoverflow.com/questions/69943176/create-a-pdf-viewer-in-jetpack-compose-using-pdfrenderer
/*@Composable
fun PdfViewer(
    modifier: Modifier = Modifier,
    uri: Uri,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp)
) {
    val rendererScope = rememberCoroutineScope()
    val mutex = remember { Mutex() }
    val renderer by produceState<PdfRenderer?>(null, uri) {
        rendererScope.launch(Dispatchers.IO) {
            val input = ParcelFileDescriptor.open(uri.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
            value = PdfRenderer(input)
        }
        awaitDispose {
            val currentRenderer = value
            rendererScope.launch(Dispatchers.IO) {
                mutex.withLock {
                    currentRenderer?.close()
                }
            }
        }
    }
    val context = LocalContext.current
    val imageLoader = LocalContext.current.imageLoader
    val imageLoadingScope = rememberCoroutineScope()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
        val height = (width * sqrt(2f)).toInt()
        val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }
        LazyColumn(
            verticalArrangement = verticalArrangement
        ) {
            items(
                count = pageCount,
                key = { index -> "$uri-$index" }
            ) { index ->
                val cacheKey = MemoryCache.Key("$uri-$index")
                var bitmap by remember { mutableStateOf(imageLoader.memoryCache?.get(cacheKey) as? Bitmap? ) }
                if (bitmap == null) {
                    DisposableEffect(uri, index) {
                        val job = imageLoadingScope.launch(Dispatchers.IO) {
                            val destinationBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            mutex.withLock {
                                Log.wtf("PdfEditor","Loading PDF $uri - page $index/$pageCount")
                                if (!coroutineContext.isActive) return@launch
                                try {
                                    renderer?.let {
                                        it.openPage(index).use { page ->
                                            page.render(
                                                destinationBitmap,
                                                null,
                                                null,
                                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    //Just catch and return in case the renderer is being closed
                                    return@launch
                                }
                            }
                            bitmap = destinationBitmap
                        }
                        onDispose {
                            job.cancel()
                        }
                    }
                    Box(modifier = Modifier
                        .background(Color.White)
                        .aspectRatio(1f / sqrt(2f))
                        .fillMaxWidth())
                }
                else {
                    val request = ImageRequest.Builder(context)
                        .size(width, height)
                        .memoryCacheKey(cacheKey)
                        .data(bitmap)
                        .build()

                    Image(
                        modifier = Modifier
                            .background(Color.White)
                            .aspectRatio(1f / sqrt(2f))
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        painter = rememberAsyncImagePainter(request),
                        contentDescription = "Page ${index + 1} of $pageCount"
                    )
                }
            }
        }
    }
}*/