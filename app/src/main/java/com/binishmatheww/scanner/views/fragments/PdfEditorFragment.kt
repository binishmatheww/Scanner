package com.binishmatheww.scanner.views.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.PdfEditor
import com.binishmatheww.scanner.common.theme.AppTheme
import com.binishmatheww.scanner.common.utils.*
import com.binishmatheww.scanner.models.PdfFile
import com.binishmatheww.scanner.viewmodels.PdfEditorViewModel
import com.binishmatheww.scanner.views.composables.PdfPageLayout
import com.binishmatheww.scanner.views.fragments.dialogs.EditorExtraDialog
import com.binishmatheww.scanner.views.fragments.dialogs.EncryptPdfDialog
import com.binishmatheww.scanner.views.fragments.dialogs.ProgressDialog
import com.binishmatheww.scanner.views.fragments.dialogs.SplitPdfDialog
import com.binishmatheww.scanner.views.listeners.*
import com.itextpdf.text.Document
import com.itextpdf.text.Element
import com.itextpdf.text.PageSize
import com.itextpdf.text.Paragraph
import com.itextpdf.text.exceptions.BadPasswordException
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.*
import java.util.*


@AndroidEntryPoint
class PdfEditorFragment : Fragment() {

    private val pdfEditorViewModel by viewModels<PdfEditorViewModel>()

    private val calendar = Calendar.getInstance()

    private var timeStamp = PdfEditor.Constants.PDF_PREFIX
        .plus(calendar.get(Calendar.YEAR))
        .plus("-")
        .plus((calendar.get(Calendar.MONTH) + 1))
        .plus("-")
        .plus(calendar.get(Calendar.DAY_OF_MONTH))
        .plus("_")
        .plus(calendar.get(Calendar.HOUR))
        .plus(":")
        .plus(calendar.get(Calendar.MINUTE))
        .plus(":")
        .plus(calendar.get(Calendar.SECOND))

    private var dialog: DialogFragment? = null

    private lateinit var addImageLauncher : ActivityResultLauncher<Intent>
    private lateinit var filePickerLauncher : ActivityResultLauncher<Intent>
    private lateinit var directoryPickerLauncher : ActivityResultLauncher<Intent>

    private var progressLiveData = MutableLiveData<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        arguments?.getBundle("images")?.serializable<ArrayList<PdfFile>>("pages")?.let{ pdfFiles ->

            lifecycleScope.launch {

               activity?.let { activity ->

                   val contentResolver = activity.contentResolver

                   for ( pdfFile in pdfFiles ) {

                       pdfFile.uri?.let { uri ->

                           try{

                               when (uri.scheme) {

                                   "file" -> {
                                       uri.toFile().inputStream()
                                   }

                                   "content" -> {
                                       contentResolver.openInputStream(uri)
                                   }

                                   else -> {
                                       null
                                   }

                               }

                           }
                           catch ( e : Exception ){ null }
                               ?.use { inputStream ->

                               pdfEditorViewModel.pdfEditor.convertImageToPdf(
                                   imageToBeConverted = inputStream.readBytes(),
                                   outputFile = File(
                                       activity.temporaryLocation().absolutePath
                                           .plus(File.separator)
                                           .plus(PdfEditor.Constants.PAGE_PREFIX)
                                           .plus(pdfFile.displayName)
                                           .plus("_")
                                           .plus(System.currentTimeMillis())
                                           .plus(PdfEditor.Constants.PAGE_EXTENSION)
                                   ),
                                   position = 0,
                                   pageSize = pdfEditorViewModel.pageSize,
                                   imageToPdfListener = object : ImageToPdfListener {
                                       override fun postExecute(result: File, position: Int) {
                                           pdfEditorViewModel.pages.add(result)
                                       }
                                   }
                               )

                           }


                       }

                   }

               }

            }

            arguments = null

        }

        arguments?.getString("uri")?.let {

            activity
                ?.contentResolver
                ?.openInputStream(Uri.parse(it))
                ?.use { inputStream ->

                    pageRenderer(inputStream.readBytes())
                    arguments = null

                }

        }

        arguments?.getString("file")?.let {
            pageRenderer(it)
            arguments = null
        }

        setFragmentResultListener("processedImage") { _: String, bundle: Bundle ->
            bundle.serializable<String>("processedImage")?.let {

                lifecycleScope.launch {

                    pdfEditorViewModel.pdfEditor.convertImageToPdf(
                        imageToBeConverted = File(it).readBytes(),
                        outputFile = File(it.plus(PdfEditor.Constants.PAGE_EXTENSION)),
                        position = pdfEditorViewModel.editAtPosition,
                        pageSize = PageSize.A4,
                        imageToPdfListener = object : ImageToPdfListener {
                            override fun postExecute(result: File, position: Int) {
                                pdfEditorViewModel.pages[position].delete()
                                pdfEditorViewModel.pages[position] = result
                            }
                        })

                }

            }
        }

        setFragmentResultListener("images") { _: String, bundle: Bundle ->
            bundle.serializable<ArrayList<File>>("pages")?.let { images ->

                lifecycleScope.launch{

                    for(image in images){
                        pdfEditorViewModel.pdfEditor.convertImageToPdf(
                            imageToBeConverted = image.readBytes(),
                            outputFile = File(image.absolutePath.plus(PdfEditor.Constants.PAGE_EXTENSION)),
                            position = 0 ,
                            pageSize = PageSize.A4,
                            imageToPdfListener = object : ImageToPdfListener {
                                override fun postExecute(result: File, position: Int) {
                                    image.delete()
                                    pdfEditorViewModel.pages.add(result)
                                }
                            }
                        )
                    }

                }

            }
        }

        setFragmentResultListener("extractedText") { _: String, bundle: Bundle ->

            bundle.serializable<String>("extractedText")?.let { string->

                var document = Document(pdfEditorViewModel.pageSize)

                document.setMargins(10f, 10f, 10f, 10f)

                activity?.let { activity ->

                    val inputPath = activity.temporaryLocation().absolutePath
                        .plus(File.separator)
                        .plus(System.currentTimeMillis().toString())
                        .plus("_Ocr")
                        .plus(PdfEditor.Constants.PAGE_EXTENSION)

                    try {
                        val writer = PdfWriter.getInstance(document, FileOutputStream(inputPath))
                        writer.setPdfVersion(PdfWriter.VERSION_1_7)
                        document.open()
                        val para = Paragraph(string.trimIndent())
                        para.alignment = Element.PARAGRAPH
                        document.add(para)
                        document.close()
                        val reader = PdfReader(inputPath)
                        for (i in 1..reader.numberOfPages) {
                            document = Document(reader.getPageSizeWithRotation(i))
                            val copy = PdfCopy(
                                document,
                                FileOutputStream(
                                    File(
                                        activity.temporaryLocation().absolutePath
                                            .plus(File.separator.toString())
                                            .plus(PdfEditor.Constants.PAGE_PREFIX)
                                            .plus(i)
                                            .plus("_")
                                            .plus(System.currentTimeMillis())
                                            .plus(PdfEditor.Constants.PAGE_EXTENSION)
                                    )
                                )
                            )
                            document.open()
                            val page = writer.getImportedPage(reader, i)
                            copy.addPage(page)
                            document.close()
                            writer.close()
                            pdfEditorViewModel.pages[pdfEditorViewModel.editAtPosition].delete()
                            pdfEditorViewModel.pages[pdfEditorViewModel.editAtPosition] = File(inputPath)
                            pdfEditorViewModel.editAtPosition++
                        }
                        pdfEditorViewModel.pages[pdfEditorViewModel.editAtPosition].delete()
                        pdfEditorViewModel.pages.removeAt(pdfEditorViewModel.editAtPosition)
                        reader.close()

                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

            }

        }

        addImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {

                activity?.let { activity->

                    val data = result.data

                    data?.getBundleExtra("bundle")?.serializable<ArrayList<File>>("pages")?.let { imagesToBeConverted ->

                        lifecycleScope.launch {

                            for (i in imagesToBeConverted.indices) {

                                pdfEditorViewModel.pdfEditor.convertImageToPdf(
                                    imageToBeConverted = imagesToBeConverted[i].readBytes(),
                                    outputFile = File(
                                        activity.temporaryLocation().absolutePath
                                            .plus(File.separator)
                                            .plus(PdfEditor.Constants.PAGE_PREFIX)
                                            .plus(i)
                                            .plus("_")
                                            .plus(System.currentTimeMillis())
                                            .plus(PdfEditor.Constants.PAGE_EXTENSION)
                                    ),
                                    position = i,
                                    pageSize = pdfEditorViewModel.pageSize,
                                    imageToPdfListener = object : ImageToPdfListener {
                                        override fun postExecute(result: File, position: Int) {
                                            //if (requestCode == IMAGE_AT_END) {
                                            pdfEditorViewModel.pages.add(result)
                                            //} else {
                                            //    pdfEditorViewModel.pages.add(pdfEditorViewModel.addImageAtPosition, result)
                                            //   addImageAtPosition++
                                            //}
                                        }
                                    }
                                )

                            }

                        }

                    }

                    data?.data?.let { dataData ->

                        activity.contentResolver?.openInputStream(dataData)?.use { inputStream ->

                            lifecycleScope.launch {

                                pdfEditorViewModel.pdfEditor.convertImageToPdf(
                                    imageToBeConverted = inputStream.readBytes(),
                                    outputFile = File(
                                        activity.temporaryLocation().absolutePath
                                            .plus(File.separator)
                                            .plus(PdfEditor.Constants.PAGE_PREFIX)
                                            .plus("_")
                                            .plus(System.currentTimeMillis())
                                            .plus(PdfEditor.Constants.PAGE_EXTENSION)
                                    ),
                                    position = pdfEditorViewModel.addImageAtPosition,
                                    pageSize = pdfEditorViewModel.pageSize,
                                    imageToPdfListener = object : ImageToPdfListener {
                                        override fun postExecute(result: File, position: Int) {
                                            //if (requestCode == IMAGE_AT_END) {
                                            pdfEditorViewModel.pages.add(result)
                                            //} else {
                                            //    pdfEditorViewModel.pages.add(addImageAtPosition, result)
                                            //}
                                        }
                                    }
                                )

                            }

                        }


                    }

                    data?.clipData?.let { clipData ->

                        var i = 0
                        while (i < clipData.itemCount) {

                            val uri = clipData.getItemAt(i)?.uri

                            uri?.let {

                                activity.contentResolver?.openInputStream(uri)?.use { inputStream ->

                                    lifecycleScope.launch {

                                        pdfEditorViewModel.pdfEditor.convertImageToPdf(
                                            imageToBeConverted = inputStream.readBytes(),
                                            outputFile = File(
                                                activity.temporaryLocation().absolutePath
                                                    .plus(PdfEditor.Constants.PAGE_PREFIX)
                                                    .plus(i)
                                                    .plus("_")
                                                    .plus(System.currentTimeMillis())
                                                    .plus(PdfEditor.Constants.PAGE_EXTENSION)
                                            ),
                                            position = i,
                                            pageSize = pdfEditorViewModel.pageSize,
                                            imageToPdfListener = object : ImageToPdfListener {
                                                override fun postExecute(
                                                    result: File,
                                                    position: Int
                                                ) {
                                                    //if (requestCode == IMAGE_AT_END) {
                                                    pdfEditorViewModel.pages.add(result)
                                                    //} else {
                                                    //   pdfEditorViewModel.pages.add(addImageAtPosition, result)
                                                    //   addImageAtPosition++
                                                    //}
                                                }
                                            })

                                    }

                                }

                            }

                            i++
                        }

                    }

                }


            }
        }

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if(result.resultCode == RESULT_OK){

                activity?.let { activity ->

                    result?.data?.data?.let{ uri ->

                        activity.contentResolver?.let { contentResolver ->

                            when {

                                contentResolver.getType(uri) == TYPE_PDF -> {

                                    contentResolver.openInputStream(uri)?.use { inputStream ->
                                        pageRenderer(inputStream.readBytes())
                                    }

                                }

                                contentResolver.getType(uri) == TYPE_TXT -> {

                                    contentResolver.openInputStream(uri)?.use { inputStream ->

                                        val name = activity.temporaryLocation().absolutePath
                                            .plus(File.separator)
                                            .plus(timeStamp)
                                            .plus(PdfEditor.Constants.PDF_EXTENSION)

                                        val document = Document(PageSize.A4)
                                        document.setMargins(10f, 10f, 10f, 10f)
                                        val writer = PdfWriter.getInstance(document, FileOutputStream(name))
                                        writer.setPdfVersion(PdfWriter.VERSION_1_7)
                                        document.open()
                                        val reader = BufferedReader(InputStreamReader(inputStream))
                                        var line: String? = null
                                        while (reader.readLine()?.also { line = it } != null) {
                                            val para = Paragraph(line?.trimIndent())
                                            para.alignment = Element.ALIGN_LEFT
                                            document.add(para)
                                        }
                                        reader.close()
                                        inputStream.close()
                                        document.close()
                                        pageRenderer(name)

                                    }

                                }

                                else -> Toast.makeText(activity,"e",Toast.LENGTH_SHORT).show()

                            }

                        }

                    }

                }

            }
        }

        directoryPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if(result.resultCode == RESULT_OK){

                result?.data?.data?.also { uri ->
                    val outputDir = DocumentFile.fromTreeUri(requireContext(),uri)
                    outputDir?.let {

                        lifecycleScope.launch {

                            pdfEditorViewModel.pdfEditor.convertPdfToImage(
                                context = activity ?: return@launch,
                                name = timeStamp,
                                outputDir = outputDir,
                                pages = pdfEditorViewModel.pages.toList(),
                                listener = object : PdfToImageListener {
                                    override fun preExecute(count: Int) {
                                        progressDialog("converting pdf to images",count)
                                    }

                                    override fun progressUpdate(progress: Int) {
                                        progressLiveData.value = progress
                                    }

                                    override fun postExecute(result: String) {
                                        dialog?.dismiss()
                                        Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
                                    }
                                })

                        }

                    }
                }

            }
        }

        return ComposeView(context = layoutInflater.context).apply {
            setContent {

                PdfEditorScreen()

            }
        }

    }

    @Composable
    private fun PdfEditorScreen(){

        AppTheme.ScannerTheme {

            val context = LocalContext.current

            val pdfEditorScope = rememberCoroutineScope()

            var isEditing by remember { mutableStateOf(false) }

            var shouldShowEditorExtraDialog by pdfEditorViewModel.shouldShowEditorExtraDialog

            LaunchedEffect(
                key1 = isEditing,
                block = {

                    context.vibrate(
                        durationInMillis = 30
                    )

                }
            )

            Scaffold(
                modifier = Modifier
                    .fillMaxSize(),
                floatingActionButton = {

                    FloatingActionButton(
                        modifier = Modifier
                            .size(64.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = {
                            shouldShowEditorExtraDialog = true
                        }
                    ){
                        Icon(
                            modifier = Modifier
                                .size(60.dp),
                            painter = painterResource(id = R.drawable.settingsic),
                            contentDescription = context.getString(R.string.Settings)
                        )
                    }

                }
            ) { paddingValues ->

                val pagePreviewState = rememberLazyListState()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding(),
                            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                        )
                        .background(MaterialTheme.colorScheme.background),
                    verticalArrangement = Arrangement.Center,
                    state = pagePreviewState,
                    content = {
                        itemsIndexed(
                            items = pdfEditorViewModel.pages,
                            key = { index, _ ->
                                pdfEditorViewModel.pages[index].absolutePath
                            }
                        ){ pageIndex, page ->

                            PdfPageLayout(
                                modifier = Modifier
                                    .padding(
                                        top = 4.dp,
                                        bottom = 4.dp,
                                    )
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                pageFile = page,
                                index = pageIndex,
                                isEditing = isEditing,
                                onLongClick = {
                                    isEditing = isEditing.not()
                                },
                                onClick = {

                                },
                                onPageUp = { position ->

                                    if (position > 0) {
                                        val f: File = pdfEditorViewModel.pages[position - 1]
                                        pdfEditorViewModel.pages[position - 1] = pdfEditorViewModel.pages[position]
                                        pdfEditorViewModel.pages[position] = f
                                        pdfEditorScope.launch {
                                            pagePreviewState.animateScrollToItem(position-1)
                                        }
                                    }

                                },
                                onPageDelete = { position->

                                    pdfEditorViewModel.pages.removeAt(position)

                                },
                                onPageDown = { position ->

                                    if (position < pdfEditorViewModel.pages.lastIndex) {
                                        val f: File = pdfEditorViewModel.pages[position]
                                        pdfEditorViewModel.pages[position] = pdfEditorViewModel.pages[position + 1]
                                        pdfEditorViewModel.pages[position + 1] = f
                                    }

                                }
                            )

                        }
                    }
                )

            }

            if(shouldShowEditorExtraDialog){

                EditorExtraDialog(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(
                            all = 8.dp
                        ),
                    onOptionSelected = {
                        when (it) {
                            "addPdf" -> addPdf()
                            "addImages" -> addImages()
                            "addText" -> addTxt()
                            "exportPdf" -> exportPdf(pdfEditorViewModel.pages.toList())
                            "splitPdf" ->  splitPdf()
                            "pdfToImages" -> pdfToImages()
                            "compressPdf" -> compressPdf()
                            "encryptPdf" -> encryptPdf()
                        }
                    },
                    onDismissRequest = {
                        shouldShowEditorExtraDialog = false
                    }
                )

            }

        }

    }



    private fun progressDialog(title: String,max : Int){
        dialog?.dismiss()
        progressLiveData.value = 0
        dialog = ProgressDialog(title,progressLiveData,max,object : OnDialogButtonClickListener {
            override fun onDialogConfirm(result: Any) {

            }

            override fun onDialogCancel() {
                progressLiveData.value = 0
            }

        })
        activity?.supportFragmentManager?.let {
            dialog?.show(it, "progressDialog")
        }
    }

    // Function to add images
    fun addImages() {

        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        addImageLauncher.launch(Intent.createChooser(intent, "Select one or more images"))


    }

    // Function to add pdf files
    private fun addPdf() {
        openFilePicker(TYPE_PDF, filePickerLauncher)
    }

    // Function to add txt files
    private fun addTxt() {
        openFilePicker(TYPE_TXT, filePickerLauncher)
    }

    // Function to export a pdf file
    private fun exportPdf(toExport: List<File>) {

        lifecycleScope.launch {

            pdfEditorViewModel.pdfEditor.mergePdf(
                pages = toExport,
                outputFile = activity?.run { File(storageLocation().absolutePath.plus(File.separator).plus(timeStamp).plus(PdfEditor.Constants.PDF_EXTENSION)) } ?: return@launch,
                mergePdfListener = object : MergePdfListener {
                    override fun onPreExecute(count: Int) {
                        progressDialog("Exporting", count)
                    }

                    override fun onProgressUpdate(progress: Int) {
                        progressLiveData.value = progress
                    }

                    override fun onPostExecute(result: String, outputPath: String) {
                        dialog?.dismiss()
                        Toast.makeText(context ?: return, result, Toast.LENGTH_SHORT).show()
                    }
                })

        }

    }

    // Function to split a pdf file
    private fun splitPdf() {

        dialog?.dismiss()

        dialog = SplitPdfDialog(object : OnDialogButtonClickListener {

            override fun onDialogConfirm(result: Any) {
                dialog?.dismiss()
                try {
                    pdfEditorViewModel.splitAtPosition = result.toString().toInt()
                    if (pdfEditorViewModel.splitAtPosition > 0 && pdfEditorViewModel.splitAtPosition < pdfEditorViewModel.pages.size) {
                        val split1: ArrayList<File> = ArrayList()
                        val split2: ArrayList<File> = ArrayList()
                        for (i in 0 until pdfEditorViewModel.splitAtPosition) {
                            split1.add(pdfEditorViewModel.pages[i])
                        }
                        for (i in pdfEditorViewModel.splitAtPosition until pdfEditorViewModel.pages.size) {
                            split2.add(pdfEditorViewModel.pages[i])
                        }
                        exportPdf(split1)
                        exportPdf(split2)
                    } else {
                        Toast.makeText(context ?: return, "Enter a number between 1 and ${pdfEditorViewModel.pages.size - 1}", Toast.LENGTH_SHORT).show()
                        splitPdf()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(context ?: return, "Enter a number", Toast.LENGTH_SHORT).show()
                    splitPdf()
                }
            }

            override fun onDialogCancel() {

            }

        })

        activity?.supportFragmentManager?.let {
            dialog?.show(it, "splitPdfDialog")
        }

    }

    // Function to convert pdf pages into images
    private fun pdfToImages() {
        openDirectoryPicker(directoryPickerLauncher)
    }

    // Function to compress a pdf file
    private fun compressPdf() {

        lifecycleScope.launch {

            pdfEditorViewModel.pdfEditor.compressPdf(
                pages = pdfEditorViewModel.pages.toList(),
                outputFile = activity?.run {

                    File(
                        storageLocation().absolutePath
                            .plus(File.separator)
                            .plus(timeStamp)
                            .plus("_compressed")
                            .plus(PdfEditor.Constants.PDF_EXTENSION))

                } ?: return@launch,
                compressionListener = object : CompressionListener {
                    override fun preExecute(count: Int) {
                        progressDialog("Compressing",count)
                    }

                    override fun progressUpdate(progress: Int) {
                        progressLiveData.value = progress
                    }

                    override fun postExecute(result: String) {
                        dialog?.dismiss()
                        Toast.makeText(context ?: return,result,Toast.LENGTH_SHORT).show()
                    }
                })

        }

    }

    // Function to encrypt a pdf
    private fun encryptPdf() {

        dialog = EncryptPdfDialog(object : OnDialogButtonClickListener {

            override fun onDialogConfirm(result: Any) {
                dialog?.dismiss()
                val inputPassword = result.toString()
                if (inputPassword.isNotEmpty()) {

                    lifecycleScope.launch {

                        pdfEditorViewModel.pdfEditor.encryptPdf(
                            pages = pdfEditorViewModel.pages.toList(),
                            outputFile = activity?.run {
                                File(
                                    storageLocation().absolutePath
                                        .plus(File.separator)
                                        .plus(timeStamp)
                                        .plus("_encrypted")
                                        .plus(PdfEditor.Constants.PDF_EXTENSION)
                                )
                            } ?: return@launch,
                            inputPassword = inputPassword,
                            masterPassword = masterPassWord,
                            encryptPdfListener = object : EncryptPdfListener {
                                override fun onPreExecute(count: Int) {
                                    progressDialog("Encrypting", count)
                                }

                                override fun onProgressUpdate(progress: Int) {
                                    progressLiveData.value = progress
                                }

                                override fun onPostExecute(result: String) {
                                    dialog?.dismiss()
                                    Toast.makeText(context ?: return, result, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                    }

                }
            }

            override fun onDialogCancel() {

            }

        })

        activity?.supportFragmentManager?.let {
            dialog?.show(it, "encryptPdfDialog")
        }

    }

    fun perspectiveCorrection(position: Int) {

        try {
            pdfEditorViewModel.editAtPosition = position
            val fd = ParcelFileDescriptor.open(
                pdfEditorViewModel.pages[position],
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(
                page.width * 2,
                page.height * 2,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.White.toArgb())
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val file = File(
                activity?.temporaryLocation() ?: return,
                PdfEditor.Constants.IMAGE_PREFIX
                    .plus(System.currentTimeMillis())
                    .plus(PdfEditor.Constants.IMAGE_EXTENSION)
            )
            file.createNewFile()
            val os: OutputStream = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.close()
            val frag = PerspectiveCorrectionFragment()
            val bundle = Bundle()
            bundle.putString("imageToBeProcessed", file.absolutePath)
            frag.arguments = bundle
            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.navigationController, frag, "PerspectiveCorrection")
                ?.addToBackStack("PerspectiveCorrection")
                ?.commitAllowingStateLoss()
        }
        catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun pageRenderer(inputPath: String) {

        lifecycleScope.launch {

            try {

                pdfEditorViewModel.pdfEditor.extractPdfPages(
                    context = context ?: return@launch,
                    sourcePdfPath = inputPath,
                    extractionListener = object : PdfPageExtractorListener {

                        override fun preExecute(count: Int) {
                            progressDialog("Rendering", count)
                        }

                        override fun progressUpdate(progress: Int) {
                            progressLiveData.value = progress
                        }

                        override fun completed(extractedPages: ArrayList<File>) {
                            dialog?.dismiss()
                            if (extractedPages.isNotEmpty()) {
                                if (pdfEditorViewModel.pages.isNotEmpty()) {
                                    var i = pdfEditorViewModel.pages.size
                                    var j = 0
                                    while (j < extractedPages.size) {
                                        pdfEditorViewModel.pages.add(i, extractedPages[j])
                                        i++
                                        j++
                                    }
                                } else {
                                    pdfEditorViewModel.pages.clear()
                                    pdfEditorViewModel.pages.addAll(extractedPages)
                                }


                            }
                        }
                    }
                )

            }
            catch (e : BadPasswordException){
                if(pdfEditorViewModel.pages.isEmpty()){
                    Toast.makeText(context ?: return@launch,"This pdf is password protected",Toast.LENGTH_SHORT).show()
                    activity?.supportFragmentManager?.popBackStack()
                }
            }

        }

    }

    private fun pageRenderer(inputBytes: ByteArray) {

        lifecycleScope.launch {

            try {
                pdfEditorViewModel.pdfEditor.extractPdfPages(
                    context = context ?: return@launch,
                    bytes = inputBytes,
                    extractionListener = object : PdfPageExtractorListener {

                        override fun preExecute(count: Int) {
                            progressDialog("Rendering", count)
                        }

                        override fun progressUpdate(progress: Int) {
                            progressLiveData.value = progress
                        }

                        override fun completed(extractedPages: ArrayList<File>) {
                            if (extractedPages.isNotEmpty()) {
                                if (pdfEditorViewModel.pages.isNotEmpty()) {
                                    var i = pdfEditorViewModel.pages.size
                                    var j = 0
                                    while (j < extractedPages.size) {
                                        pdfEditorViewModel.pages.add(i, extractedPages[j])
                                        i++
                                        j++
                                    }
                                } else {
                                    pdfEditorViewModel.pages.clear()
                                    pdfEditorViewModel.pages.addAll(extractedPages)
                                }
                                dialog?.dismiss()
                            }
                        }
                    }
                )
            }
            catch (e : BadPasswordException){
                if(pdfEditorViewModel.pages.isEmpty()){
                    Toast.makeText(context ?: return@launch,"This pdf is password protected",Toast.LENGTH_SHORT).show()
                    activity?.supportFragmentManager?.popBackStack()
                }
            }

        }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            result?.uri?.let { uri ->

                activity
                    ?.contentResolver
                    ?.openInputStream(uri)
                    ?.use { inputStream ->

                        lifecycleScope.launch {

                            pdfEditorViewModel.pdfEditor.convertImageToPdf(
                                imageToBeConverted = inputStream.readBytes(),
                                outputFile = File(
                                    activity?.temporaryLocation()?.absolutePath
                                        ?.plus(PdfEditor.Constants.PAGE_PREFIX)
                                        ?.plus(pdfEditorViewModel.editAtPosition.toString())
                                        ?.plus("_")
                                        ?.plus(System.currentTimeMillis())
                                        ?.plus(PdfEditor.Constants.PAGE_EXTENSION) ?: return@launch
                                ),
                                position = pdfEditorViewModel.editAtPosition,
                                pageSize = pdfEditorViewModel.pageSize,
                                imageToPdfListener = object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        pdfEditorViewModel.pages[pdfEditorViewModel.editAtPosition].delete()
                                        pdfEditorViewModel.pages[pdfEditorViewModel.editAtPosition] = result
                                    }
                                }
                            )

                        }

                    }

            }
        }


    }

    private val pageClickListener =  object : PageClickListener {

        override fun pageClicked(position: Int) {

        }

        override fun deletePage(position: Int) {
            pdfEditorViewModel.pages[position].delete()
            pdfEditorViewModel.pages.removeAt(position)
        }

        override fun rotate(position: Int, rotation: Int) {

            lifecycleScope.launch {

                pdfEditorViewModel.pdfEditor.rotatePdf(
                    page = pdfEditorViewModel.pages[position],
                    position = position,
                    rotation = rotation,
                    outputFile = File(
                        activity?.run {
                            temporaryLocation().absolutePath
                                .plus(System.currentTimeMillis())
                                .plus("_rotated")
                                .plus(PdfEditor.Constants.PDF_EXTENSION)
                        } ?: return@launch
                    ),
                    rotatePageListener = object : RotatePageListener {
                        override fun postExecute(position: Int, rotatedPage: File) {
                            pdfEditorViewModel.pages[position].delete()
                            pdfEditorViewModel.pages[position] = rotatedPage
                        }
                    })

            }

        }

        override fun crop(position: Int) {
            pdfEditorViewModel.editAtPosition = position
            try {
                val fd = ParcelFileDescriptor.open(
                    pdfEditorViewModel.pages[position],
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                val renderer = PdfRenderer(fd)
                val page = renderer.openPage(0)
                val bitmap = Bitmap.createBitmap(
                    page.width * 2,
                    page.height * 2,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.White.toArgb())
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                val file = activity?.run {
                    File(
                        temporaryLocation(),
                        PdfEditor.Constants.IMAGE_PREFIX
                            .plus(System.currentTimeMillis())
                            .plus(PdfEditor.Constants.IMAGE_EXTENSION)
                    )
                } ?: return
                file.createNewFile()
                val os: OutputStream = BufferedOutputStream(FileOutputStream(file))
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os.close()
                Intent()
                CropImage.activity(Uri.fromFile(file))
                    .start(context ?: return, this@PdfEditorFragment)
                //CropImage.activity(Uri.fromFile(file)).setAspectRatio(((int) pageSize.getWidth()), ((int) pageSize.getHeight())).start(context ?: return,this@PdfEditorFragment);
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        override fun ocr(position: Int) {
            pdfEditorViewModel.editAtPosition = position
            val bundle = Bundle()
            bundle.putString("filePath", pdfEditorViewModel.pages[pdfEditorViewModel.editAtPosition].absolutePath)
            val frag = OcrFragment()
            frag.arguments = bundle
            activity
                ?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.navigationController, frag, "Ocr")
                ?.addToBackStack("Ocr")
                ?.commitAllowingStateLoss()

        }

        override fun filter(position: Int, key: Float) {

            lifecycleScope.launch {

                pdfEditorViewModel.pdfEditor.filterImage(
                    key = key,
                    inputPage = pdfEditorViewModel.pages[position],
                    filteredImage = activity?.run {
                        File(
                            temporaryLocation(),
                            PdfEditor.Constants.IMAGE_PREFIX
                                .plus(System.currentTimeMillis())
                                .plus("_filtered" )
                                .plus(PdfEditor.Constants.IMAGE_EXTENSION)
                        )
                    } ?: return@launch,
                    filterImageListener = object : FilterImageListener {
                        override suspend fun postExecute(filteredImage: File) {

                            pdfEditorViewModel.pdfEditor.convertImageToPdf(
                                imageToBeConverted = filteredImage.readBytes(),
                                outputFile = File(filteredImage.absolutePath.plus(PdfEditor.Constants.PDF_EXTENSION)),
                                position = position,
                                pageSize = PageSize.A4,
                                imageToPdfListener = object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        filteredImage.delete()
                                        pdfEditorViewModel.pages[position].delete()
                                        pdfEditorViewModel.pages[position] = result
                                    }

                                })

                        }
                    }
                )

            }

        }

        override fun up(position: Int) {
            if (position > 0) {
                val f: File = pdfEditorViewModel.pages[position - 1]
                pdfEditorViewModel.pages[position - 1] = pdfEditorViewModel.pages[position]
                pdfEditorViewModel.pages[position] = f
            }
        }

        override fun down(position: Int) {
            if (position < pdfEditorViewModel.pages.size - 1) {
                val f: File = pdfEditorViewModel.pages[position]
                pdfEditorViewModel.pages[position] = pdfEditorViewModel.pages[position + 1]
                pdfEditorViewModel.pages[position + 1] = f
            }
        }

    }

}

