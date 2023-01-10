package com.binishmatheww.scanner.views.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.PdfEditor
import com.binishmatheww.scanner.common.theme.AppTheme
import com.binishmatheww.scanner.common.utils.*
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
import kotlinx.coroutines.launch
import java.io.*
import java.util.*


class PdfEditorFragment : Fragment() {

    private val pdfEditor by lazy{ PdfEditor() }

    private lateinit var timeStamp : String
    private var pages = mutableStateListOf<File>()
    private var pageSize = getPageSize("DEFAULT (A4)")

    private var editAtPosition = 0
    private var splitAtPosition = 0
    private var addImageAtPosition = 0

    private var dialog: DialogFragment? = null

    private lateinit var addImageLauncher : ActivityResultLauncher<Intent>
    private lateinit var filePickerLauncher : ActivityResultLauncher<Intent>
    private lateinit var directoryPickerLauncher : ActivityResultLauncher<Intent>

    private var progressLiveData = MutableLiveData<Int>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val calendar = Calendar.getInstance()

        timeStamp = getString(R.string.pdf_prefix) +
                calendar.get(Calendar.YEAR)+"-"+
                (calendar.get(Calendar.MONTH)+1)+"-"+
                calendar.get(Calendar.DAY_OF_MONTH)+"_"+
                calendar.get(Calendar.HOUR)+":"+
                calendar.get(Calendar.MINUTE)+":"+
                calendar.get(Calendar.SECOND)

        arguments?.getBundle("images")?.serializable<ArrayList<File>>("pages")?.let{ images ->

            lifecycleScope.launch {

                for ( image in images) {

                    pdfEditor.convertImageToPdf(
                        imageToBeConverted = image.readBytes(),
                        outputFile = File(
                            temporaryLocation(context).absolutePath +
                                    File.separator +
                                    getString(R.string.page_prefix) +
                                    image.nameWithoutExtension +
                                    "_" +
                                    System.currentTimeMillis()
                                    + getString(R.string.page_extension)
                        ),
                        position = 0,
                        pageSize = pageSize,
                        imageToPdfListener = object : ImageToPdfListener {
                            override fun postExecute(result: File, position: Int) {
                                pages.add(result)
                                //TODO renderingAdapter.data(pages)
                            }
                        }
                    )

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

                    pdfEditor.convertImageToPdf(
                        imageToBeConverted = File(it).readBytes(),
                        outputFile = File(it.plus(getString(R.string.page_extension))),
                        position = editAtPosition,
                        pageSize = getPageSize("DEFAULT (A4)"),
                        imageToPdfListener = object : ImageToPdfListener {
                            override fun postExecute(result: File, position: Int) {
                                pages[position].delete()
                                pages[position] = result
                                //TODO renderingAdapter.data(pages)
                            }
                        })

                }

            }
        }

        setFragmentResultListener("images") { _: String, bundle: Bundle ->
            bundle.serializable<ArrayList<File>>("pages")?.let { images ->

                lifecycleScope.launch{

                    for(image in images){
                        pdfEditor.convertImageToPdf(
                            imageToBeConverted = image.readBytes(),
                            outputFile = File(image.absolutePath.plus(getString(R.string.page_extension))),
                            position = 0 ,
                            pageSize = getPageSize("DEFAULT (A4)"),
                            imageToPdfListener = object : ImageToPdfListener {
                                override fun postExecute(result: File, position: Int) {
                                    image.delete()
                                    pages.add(result)
                                    //TODO renderingAdapter.data(pages)
                                }
                            }
                        )
                    }

                }

            }
        }

        setFragmentResultListener("extractedText") { _: String, bundle: Bundle ->

            bundle.serializable<String>("extractedText")?.let { string->

                var document = Document(pageSize)

                document.setMargins(10f, 10f, 10f, 10f)

                val inputPath = temporaryLocation(requireContext()).absolutePath +
                        File.separator +
                        System.currentTimeMillis().toString() +
                        "_Ocr" +
                        getString(R.string.page_extension)

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
                                    temporaryLocation( activity ?: return@setFragmentResultListener ).absolutePath +
                                            File.separator.toString() +
                                            getString(R.string.page_prefix) +
                                            i +
                                            "_" +
                                            System.currentTimeMillis() +
                                            getString(R.string.page_extension)
                                )
                            )
                        )
                        document.open()
                        val page = writer.getImportedPage(reader, i)
                        copy.addPage(page)
                        document.close()
                        writer.close()
                        pages[editAtPosition].delete()
                        pages[editAtPosition] = File(inputPath)
                        editAtPosition++
                    }
                    pages[editAtPosition].delete()
                    pages.removeAt(editAtPosition)
                    reader.close()

                    //TODO renderingAdapter.data(pages)
                }
                catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        }

        addImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {

                val data = result.data

                data?.getBundleExtra("bundle")?.serializable<ArrayList<File>>("pages")?.let { imagesToBeConverted ->

                    lifecycleScope.launch {

                        for (i in imagesToBeConverted.indices) {

                            pdfEditor.convertImageToPdf(
                                imageToBeConverted = imagesToBeConverted[i].readBytes(),
                                outputFile = File(
                                    temporaryLocation( activity ?: return@launch ).absolutePath +
                                            File.separator +
                                            getString(R.string.page_prefix) +
                                            i.toString() +
                                            "_" +
                                            System.currentTimeMillis() +
                                            getString(R.string.page_extension)
                                ),
                                position = i,
                                pageSize = pageSize,
                                imageToPdfListener = object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        //if (requestCode == IMAGE_AT_END) {
                                        pages.add(result)
                                        //} else {
                                        //    pages.add(addImageAtPosition, result)
                                        //   addImageAtPosition++
                                        //}
                                        //TODO renderingAdapter.data(pages)
                                    }
                                })

                        }

                    }

                }

                data?.data?.let { dataData ->

                    activity?.contentResolver?.openInputStream(dataData)?.use { inputStream ->

                        lifecycleScope.launch {

                            pdfEditor.convertImageToPdf(
                                imageToBeConverted = inputStream.readBytes(),
                                outputFile = File(
                                    temporaryLocation( activity ?: return@launch ).absolutePath +
                                            File.separator +
                                            getString(R.string.page_prefix) + "_" +
                                            System.currentTimeMillis() +
                                            getString(R.string.page_extension)
                                ),
                                position = addImageAtPosition,
                                pageSize = pageSize,
                                imageToPdfListener = object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        //if (requestCode == IMAGE_AT_END) {
                                        pages.add(result)
                                        //} else {
                                        //    pages.add(addImageAtPosition, result)
                                        //}
                                        //TODO renderingAdapter.data(pages)
                                    }
                                })

                        }

                    }


                }

                data?.clipData?.let { clipData ->
                    var i = 0
                    while (i < clipData.itemCount) {

                        val uri = clipData.getItemAt(i)?.uri

                        uri?.let {

                            activity?.contentResolver?.openInputStream(uri)?.use { inputStream ->

                                lifecycleScope.launch {

                                    pdfEditor.convertImageToPdf(
                                        imageToBeConverted = inputStream.readBytes(),
                                        outputFile = File(
                                            temporaryLocation( activity ?: return@launch ).absolutePath +
                                                    getString(R.string.page_prefix) +
                                                    i.toString() +
                                                    "_" +
                                                    System.currentTimeMillis() +
                                                    getString(R.string.page_extension)
                                        ),
                                        position = i,
                                        pageSize = pageSize,
                                        imageToPdfListener = object : ImageToPdfListener {
                                            override fun postExecute(
                                                result: File,
                                                position: Int
                                            ) {
                                                //if (requestCode == IMAGE_AT_END) {
                                                pages.add(result)
                                                //} else {
                                                //   pages.add(addImageAtPosition, result)
                                                //   addImageAtPosition++
                                                //}
                                                //TODO renderingAdapter.data(pages)
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

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if(result.resultCode == RESULT_OK){

                result?.data?.data?.let{ uri ->

                    activity?.contentResolver?.let { contentResolver ->

                        when {

                            contentResolver.getType(uri) == TYPE_PDF -> {

                                contentResolver.openInputStream(uri)?.use { inputStream ->
                                    pageRenderer(inputStream.readBytes())
                                }

                            }

                            contentResolver.getType(uri) == TYPE_TXT -> {

                                contentResolver.openInputStream(uri)?.use { inputStream ->

                                    val name = temporaryLocation(requireContext()).absolutePath +
                                            File.separator +
                                            timeStamp +
                                            getString(R.string.pdf_extension)
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

                            else -> Toast.makeText( activity ?: return@registerForActivityResult,"e",Toast.LENGTH_SHORT).show()

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

                            pdfEditor.convertPdfToImage(
                                context = activity ?: return@launch,
                                name = timeStamp,
                                outputDir = outputDir,
                                pages = pages.toList(),
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

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return ComposeView(context = layoutInflater.context).apply {
            setContent {

                PdfEditorScreen()

            }
        }

    }

    @Composable
    private fun PdfEditorScreen(){

        AppTheme.ScannerTheme {

            var isEditing by remember { mutableStateOf(false) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                content = {
                    itemsIndexed(pages){ index, page ->
                        PagePreview(
                            modifier = Modifier
                                .padding(
                                    top = if (pages.size > 1 && index == 0) 100.dp else 0.dp,
                                    bottom = if (pages.size > 1 && index == pages.lastIndex) 100.dp else 0.dp,
                                )
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            pageFile = page,
                            isEditing = isEditing,
                            onClick = {
                                isEditing = isEditing.not()
                            }
                        )
                    }
                }
            )

        }

    }

    @Composable
    private fun PagePreview(
        modifier: Modifier = Modifier,
        pageFile: File,
        isEditing: Boolean,
        onClick: () -> Unit
    ){

        val bitmap by remember(key1 = pageFile) { mutableStateOf(pageFile.getPdfPreview()) }

        Box(
            modifier = modifier
        ){

            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        onClick.invoke()
                    },
                bitmap = bitmap?.asImageBitmap() ?: return,
                contentDescription = pageFile.name,
                contentScale = ContentScale.FillWidth
            )

            AnimatedVisibility(
                modifier = Modifier
                    .fillMaxSize(),
                visible = isEditing,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 2000
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = 2000
                    )
                )
            ) {

                ConstraintLayout(
                    modifier = Modifier
                        .fillMaxSize()
                ) {

                    val (
                        testConstraint
                    ) = createRefs()

                    Text(
                        modifier = Modifier
                            .constrainAs(testConstraint){
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                top.linkTo(parent.top)
                                bottom.linkTo(parent.bottom)
                        }.wrapContentSize(),
                        text = "Test",
                        color = Color.Blue,
                        fontSize = 32.sp
                    )

                }

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

    private fun editorExtraDialog(){
        dialog?.dismiss()
        dialog = EditorExtraDialog(object : OnDialogButtonClickListener {

            override fun onDialogConfirm(result: Any) {
                dialog?.dismiss()
                result.toString().let {
                    when (it) {
                        "addPdf" -> addPdf()
                        "addImages" -> addImages()
                        "addText" -> addTxt()

                        "exportPdf" -> exportPdf(pages.toList())
                        "splitPdf" ->  splitPdf()
                        "pdfToImages" -> pdfToImages()
                        "compressPdf" -> compressPdf()
                        "encryptPdf" -> encryptPdf()
                    }
                }
            }

            override fun onDialogCancel() {

            }

        })
        activity?.supportFragmentManager?.let {
            dialog?.show(it, "editorExtraDialog")
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

            pdfEditor.mergePdf(
                pages = toExport,
                outputFile = File(storageLocation( activity ?: return@launch ).absolutePath.plus(File.separator).plus(timeStamp).plus(activity?.getString(R.string.pdf_extension))),
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
                    splitAtPosition = result.toString().toInt()
                    if (splitAtPosition > 0 && splitAtPosition < pages.size) {
                        val split1: ArrayList<File> = ArrayList()
                        val split2: ArrayList<File> = ArrayList()
                        for (i in 0 until splitAtPosition) {
                            split1.add(pages[i])
                        }
                        for (i in splitAtPosition until pages.size) {
                            split2.add(pages[i])
                        }
                        exportPdf(split1)
                        exportPdf(split2)
                    } else {
                        Toast.makeText(context ?: return, "Enter a number between 1 and ${pages.size - 1}", Toast.LENGTH_SHORT).show()
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

            pdfEditor.compressPdf(
                pages = pages.toList(),
                outputFile = File(
                    storageLocation( activity ?: return@launch ).absolutePath
                        .plus(File.separator)
                        .plus(timeStamp)
                        .plus("_compressed")
                        .plus(context?.getString(R.string.pdf_extension))
                ),
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

                        pdfEditor.encryptPdf(
                            pages = pages.toList(),
                            outputFile = File(
                                storageLocation( activity ?: return@launch ).absolutePath
                                    .plus(File.separator)
                                    .plus(timeStamp)
                                    .plus("_encrypted")
                                    .plus(activity?.getString(R.string.pdf_extension))
                            ),
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
            editAtPosition = position
            val fd = ParcelFileDescriptor.open(
                pages[position],
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
                temporaryLocation(context ?: return),
                getString(R.string.image_prefix) + System.currentTimeMillis() + getString(
                    R.string.image_extension
                )
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

                pdfEditor.extractPdfPages(
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
                                if (pages.isNotEmpty()) {
                                    var i = pages.size
                                    var j = 0
                                    while (j < extractedPages.size) {
                                        pages.add(i, extractedPages[j])
                                        i++
                                        j++
                                    }
                                } else {
                                    pages.clear()
                                    pages.addAll(extractedPages)
                                }

                                //TODO renderingAdapter.data(pages)
                            }
                        }
                    }
                )

            }
            catch (e : BadPasswordException){
                if(pages.isEmpty()){
                    Toast.makeText(context ?: return@launch,"This pdf is password protected",Toast.LENGTH_SHORT).show()
                    activity?.supportFragmentManager?.popBackStack()
                }
            }

        }

    }

    private fun pageRenderer(inputBytes: ByteArray) {

        lifecycleScope.launch {

            try {
                pdfEditor.extractPdfPages(
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
                                if (pages.isNotEmpty()) {
                                    var i = pages.size
                                    var j = 0
                                    while (j < extractedPages.size) {
                                        pages.add(i, extractedPages[j])
                                        i++
                                        j++
                                    }
                                } else {
                                    pages.clear()
                                    pages.addAll(extractedPages)
                                }
                                //TODO renderingAdapter.data(pages)
                                dialog?.dismiss()
                            }
                        }
                    }
                )
            }
            catch (e : BadPasswordException){
                if(pages.isEmpty()){
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

                            pdfEditor.convertImageToPdf(
                                imageToBeConverted = inputStream.readBytes(),
                                outputFile = File(
                                    temporaryLocation(context ?: return@launch).absolutePath +
                                            getString(R.string.page_prefix)
                                            + editAtPosition.toString() +
                                            "_" +
                                            System.currentTimeMillis() +
                                            getString(R.string.page_extension)
                                ),
                                position = editAtPosition,
                                pageSize = pageSize,
                                imageToPdfListener = object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        pages[editAtPosition].delete()
                                        pages[editAtPosition] = result
                                        //TODO renderingAdapter.data(pages)
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
            pages[position].delete()
            pages.removeAt(position)
            //TODO renderingAdapter.data(pages)
        }

        override fun rotate(position: Int, rotation: Int) {

            lifecycleScope.launch {

                pdfEditor.rotatePdf(
                    page = pages[position],
                    position = position,
                    rotation = rotation,
                    outputFile = File(
                        temporaryLocation( activity ?: return@launch).absolutePath +
                            System.currentTimeMillis() +
                            "_rotated" +
                            activity?.getString(R.string.page_extension)
                    ),
                    rotatePageListener = object : RotatePageListener {
                        override fun postExecute(position: Int, rotatedPage: File) {
                            pages[position].delete()
                            pages[position] = rotatedPage
                            //TODO renderingAdapter.data(pages)
                        }
                    })

            }

        }

        override fun crop(position: Int) {
            editAtPosition = position
            try {
                val fd = ParcelFileDescriptor.open(
                    pages[position],
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
                    temporaryLocation(context ?: return),
                    getString(R.string.image_prefix) + System.currentTimeMillis() + getString(R.string.image_extension)
                )
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
            editAtPosition = position
            val bundle = Bundle()
            bundle.putString("filePath", pages[editAtPosition].absolutePath)
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

                pdfEditor.filterImage(
                    key = key,
                    inputPage = pages[position],
                    filteredImage = File(
                        temporaryLocation( activity ?: return@launch),
                        context?.getString(R.string.image_prefix) +
                                System.currentTimeMillis() +
                                "_filtered" +
                                context?.getString(R.string.image_extension)
                    ),
                    filterImageListener = object : FilterImageListener {
                        override suspend fun postExecute(filteredImage: File) {

                            pdfEditor.convertImageToPdf(
                                imageToBeConverted = filteredImage.readBytes(),
                                outputFile = File(filteredImage.absolutePath.plus(R.string.pdf_extension)),
                                position = position,
                                pageSize = getPageSize("DEFAULT (A4)"),
                                imageToPdfListener = object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        filteredImage.delete()
                                        pages[position].delete()
                                        pages[position] = result
                                        //TODO renderingAdapter.data(pages)
                                    }

                                })

                        }
                    }
                )

            }

        }

        override fun up(position: Int) {
            if (position > 0) {
                val f: File = pages[position - 1]
                pages[position - 1] = pages[position]
                pages[position] = f
                //TODO renderingAdapter.data(pages)
                //TODO renderingView.scrollToPosition(position)
            }
        }

        override fun down(position: Int) {
            if (position < pages.size - 1) {
                val f: File = pages[position]
                pages[position] = pages[position + 1]
                pages[position + 1] = f
                //TODO renderingAdapter.data(pages)
                //TODO renderingView.scrollToPosition(position)
            }
        }

    }

}

