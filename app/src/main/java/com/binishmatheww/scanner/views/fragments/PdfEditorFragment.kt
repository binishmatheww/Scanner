package com.binishmatheww.scanner.views.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.PdfEditor
import com.binishmatheww.scanner.views.adapters.PageAdapter
import com.binishmatheww.scanner.views.fragments.dialogs.EditorExtraDialog
import com.binishmatheww.scanner.views.fragments.dialogs.EncryptPdfDialog
import com.binishmatheww.scanner.views.fragments.dialogs.ProgressDialog
import com.binishmatheww.scanner.views.fragments.dialogs.SplitPdfDialog
import com.binishmatheww.scanner.views.listeners.*
import com.binishmatheww.scanner.views.utils.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.itextpdf.text.*
import com.itextpdf.text.exceptions.BadPasswordException
import com.itextpdf.text.pdf.PdfCopy
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfWriter
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.*
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


class PdfEditorFragment : Fragment() {

    private lateinit var timeStamp : String
    private var pages = ArrayList<File>()
    private var pageSize = getPageSize("DEFAULT (A4)")

    private var editAtPosition = 0
    private var splitAtPosition = 0
    private var addImageAtPosition = 0

    private lateinit var renderingView : RecyclerView
    private lateinit var renderingAdapter: PageAdapter

    private lateinit var editorExtraActionButton : FloatingActionButton

    private var dialog: DialogFragment? = null

    private lateinit var addImageLauncher : ActivityResultLauncher<Intent>
    private lateinit var filePickerLauncher : ActivityResultLauncher<Intent>
    private lateinit var directoryPickerLauncher : ActivityResultLauncher<Intent>

    private var progressLiveData = MutableLiveData<Int>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.fragment_pdf_editor, container, false)
    }

    override fun onViewCreated(layout : View, savedInstanceState: Bundle?) {
        super.onViewCreated(layout, savedInstanceState)

        renderingView = layout.findViewById(R.id.renderingView)
        editorExtraActionButton = layout.findViewById(R.id.editorExtraActionButton)

        progressLiveData.value = 0

        editorExtraActionButton.setOnClickListener {
            editorExtraDialog()
        }

        val calendar = Calendar.getInstance()

        timeStamp = getString(R.string.pdf_prefix) +
                calendar.get(Calendar.YEAR)+"-"+
                (calendar.get(Calendar.MONTH)+1)+"-"+
                calendar.get(Calendar.DAY_OF_MONTH)+"_"+
                calendar.get(Calendar.HOUR)+":"+
                calendar.get(Calendar.MINUTE)+":"+
                calendar.get(Calendar.SECOND)


        initializeEditor()




    }

    fun dialogModel(){
        dialog = EncryptPdfDialog(object : OnDialogButtonClickListener {

            override fun onDialogConfirm(result: Any) {
                dialog?.dismiss()
            }

            override fun onDialogCancel() {

            }

        })
        requireActivity().supportFragmentManager.let {
            dialog?.show(it, "Dialog")
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
        requireActivity().supportFragmentManager.let {
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

                        "exportPdf" -> exportPdf(pages)
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
        requireActivity().supportFragmentManager.let {
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
    private fun exportPdf(toExport: ArrayList<File>) {

        MergePdf(
            context = context ?: return,
            pages = toExport,
            outputName = timeStamp,
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

            PdfEditor.compressPdf(
                context = context ?: return@launch,
                pages = pages,
                outputName = timeStamp,
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
                    EncryptPdf(
                        context = context ?: return,
                        pages = pages,
                        outputName = timeStamp,
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

            override fun onDialogCancel() {

            }

        })

        activity?.supportFragmentManager?.let {
            dialog?.show(it, "encryptPdfDialog")
        }

    }

    private fun pageRenderer(inputPath: String) {

        try {
            PdfPageExtractor(
                context = context ?: return,
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
                                pages = extractedPages
                            }

                            renderingAdapter.data(pages)
                        }
                    }
                }
            )
        }catch (e : BadPasswordException){
            if(pages.isEmpty()){
                Toast.makeText(context ?: return,"This pdf is password protected",Toast.LENGTH_SHORT).show()
                activity?.supportFragmentManager?.popBackStack()
            }
        }
    }

    private fun pageRenderer(inputBytes: ByteArray) {
        try {
            PdfPageExtractor(
                context = context ?: return,
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
                                pages = extractedPages
                            }
                            renderingAdapter.data(pages)
                            dialog?.dismiss()
                        }
                    }
                }
            )
        }catch (e : BadPasswordException){
            if(pages.isEmpty()){
                Toast.makeText(context ?: return,"This pdf is password protected",Toast.LENGTH_SHORT).show()
                activity?.supportFragmentManager?.popBackStack()
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

                        ImageToPdf(
                            context = context ?: return,
                            imageToBeConverted = inputStream.readBytes(),
                            outputPath = temporaryLocation(context ?: return).absolutePath + getString(R.string.page_prefix) + editAtPosition.toString() + "_" + System.currentTimeMillis() + getString(R.string.page_extension),
                            position = editAtPosition,
                            pageSize = pageSize,
                            imgToPdfListener = object : ImageToPdfListener {
                                override fun postExecute(result: File, position: Int) {
                                    pages[editAtPosition].delete()
                                    pages[editAtPosition] = result
                                    renderingAdapter.data(pages)
                                }
                            }
                        )

                    }

            }
        }


    }

    @Suppress("UNCHECKED_CAST")
    private fun initializeEditor() {

        renderingAdapter = PageAdapter(context ?: return, pages,pageClickListener)
        renderingAdapter.setHasStableIds(true)
        renderingView.layoutManager = LinearLayoutManager(context ?: return)
        renderingView.adapter = renderingAdapter
        //renderingView.setItemViewCacheSize(30)

        arguments?.getBundle("images")?.serializable<ArrayList<File>>("pages")?.let{ images ->

            for ( image in images) {

                ImageToPdf(
                    context = context ?: return,
                    imageToBeConverted = image.readBytes(),
                    outputPath = temporaryLocation(context ?: return).absolutePath + File.separator + getString(R.string.page_prefix) + image.nameWithoutExtension + "_" + System.currentTimeMillis() + getString(R.string.page_extension),
                    position = 0,
                    pageSize = pageSize,
                    imgToPdfListener = object : ImageToPdfListener {
                        override fun postExecute(result: File, position: Int) {
                            pages.add(result)
                            renderingAdapter.data(pages)
                        }
                    }
                )

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

    }

    private val pageClickListener =  object : PageClickListener {

        override fun pageClicked(position: Int) {

        }


        override fun deletePage(position: Int) {
            pages[position].delete()
            pages.removeAt(position)
            renderingAdapter.data(pages)
        }

        override fun rotate(position: Int, rotation: Int) {
            RotatePage(
                context = context ?: return,
                page = pages[position],
                position = position,
                rotation = rotation,
                rotatePageListener = object : RotatePageListener {
                    override fun postExecute(position: Int, rotatedPage: File) {
                        pages[position].delete()
                        pages[position] = rotatedPage
                        renderingAdapter.data(pages)
                    }
                })
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
                canvas.drawColor(Color.WHITE)
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
                canvas.drawColor(Color.WHITE)
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
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.navigationController, frag, "PerspectiveCorrection")
                    .addToBackStack("PerspectiveCorrection").commitAllowingStateLoss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun ocr(position: Int) {
            editAtPosition = position
            val bundle = Bundle()
            bundle.putString("filePath", pages[editAtPosition].absolutePath)
            val frag = OcrFragment()
            frag.arguments = bundle
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.navigationController, frag, "Ocr").addToBackStack("Ocr")
                .commitAllowingStateLoss()

        }


        override fun filter(position: Int, key: Float) {

            lifecycleScope.launch {

                PdfEditor.filterImage(
                    context = context ?: return@launch,
                    key = key,
                    inputPage = pages[position],
                    filterImageListener = object : FilterImageListener {
                        override fun postExecute(filteredImage: File) {
                            ImageToPdf(context ?: return,
                                filteredImage.readBytes(),
                                filteredImage.absolutePath.plus(R.string.pdf_extension),
                                position,
                                getPageSize("DEFAULT (A4)"),
                                object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        filteredImage.delete()
                                        pages[position].delete()
                                        pages[position] = result
                                        renderingAdapter.data(pages)
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
                renderingAdapter.data(pages)
                renderingView.scrollToPosition(position)
            }
        }

        override fun down(position: Int) {
            if (position < pages.size - 1) {
                val f: File = pages[position]
                pages[position] = pages[position + 1]
                pages[position + 1] = f
                renderingAdapter.data(pages)
                renderingView.scrollToPosition(position)
            }
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)

        setFragmentResultListener("processedImage") { _: String, bundle: Bundle ->
            bundle.serializable<String>("processedImage")?.let {
                ImageToPdf(requireContext(), File(it).readBytes(), it.plus(getString(R.string.page_extension)), editAtPosition, getPageSize("DEFAULT (A4)"),
                    object : ImageToPdfListener {
                        override fun postExecute(result: File, position: Int) {
                            pages[position].delete()
                            pages[position] = result
                            renderingAdapter.data(pages)
                        }
                    })
            }
        }

        setFragmentResultListener("images") { _: String, bundle: Bundle ->
            bundle.serializable<ArrayList<File>>("pages")?.let { images ->
                for(image in images){
                    ImageToPdf(
                        context = context,
                        imageToBeConverted = image.readBytes(),
                        outputPath = image.absolutePath.plus(getString(R.string.page_extension)),
                        position = 0 ,
                        pageSize = getPageSize("DEFAULT (A4)"),
                        imgToPdfListener = object : ImageToPdfListener {
                            override fun postExecute(result: File, position: Int) {
                                image.delete()
                                pages.add(result)
                                renderingAdapter.data(pages)
                            }
                        }
                    )
                }
            }
        }

        setFragmentResultListener("extractedText") { _: String, bundle: Bundle ->
            bundle.serializable<String>("extractedText")?.let { string->
                var document = Document(pageSize)
                document.setMargins(10f, 10f, 10f, 10f)
                val inputPath =
                    temporaryLocation(requireContext()).absolutePath + File.separator + System.currentTimeMillis().toString() + "_Ocr" + getString(
                        R.string.page_extension
                    )
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
                        val outputPdfName = getString(R.string.page_prefix) + i + "_" + System.currentTimeMillis() + getString(
                            R.string.page_extension
                        )
                        document = Document(reader.getPageSizeWithRotation(i))
                        val pageFile =
                            File(temporaryLocation(requireContext()).absolutePath + File.separator.toString() + outputPdfName)
                        val copy = PdfCopy(document, FileOutputStream(pageFile))
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

                    renderingAdapter.data(pages)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        addImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                data?.getBundleExtra("bundle")?.let {

                    val imagesToBeConverted = (it.getSerializable("pages") as ArrayList<File>)
                    for (i in imagesToBeConverted.indices) {
                        val op: String = temporaryLocation(requireContext()).absolutePath + File.separator + getString(R.string.page_prefix) + i.toString() + "_" + System.currentTimeMillis() + getString(R.string.page_extension)
                        ImageToPdf(requireContext(),
                            imagesToBeConverted[i].readBytes(),
                            op,
                            i,
                            pageSize,
                            object : ImageToPdfListener {
                                override fun postExecute(result: File, position: Int) {
                                    //if (requestCode == IMAGE_AT_END) {
                                    pages.add(result)
                                    //} else {
                                    //    pages.add(addImageAtPosition, result)
                                    //   addImageAtPosition++
                                    //}
                                    renderingAdapter.data(pages)
                                }
                            })
                    }

                }
                data?.data?.let {
                    requireActivity().contentResolver.openInputStream(it)?.readBytes()
                        ?.let { bytes ->
                            val op: String =
                                temporaryLocation(requireContext()).absolutePath + File.separator + getString(
                                    R.string.page_prefix
                                ) + "_" + System.currentTimeMillis() + getString(R.string.page_extension)
                            ImageToPdf(
                                requireContext(),
                                bytes,
                                op,
                                addImageAtPosition,
                                pageSize,
                                object : ImageToPdfListener {
                                    override fun postExecute(result: File, position: Int) {
                                        //if (requestCode == IMAGE_AT_END) {
                                        pages.add(result)
                                        //} else {
                                        //    pages.add(addImageAtPosition, result)
                                        //}
                                        renderingAdapter.data(pages)
                                    }
                                })
                        }
                }
                data?.clipData?.let {
                    var i = 0
                    while (i < it.itemCount) {
                        val uri = it.getItemAt(i)?.uri
                        uri?.let {
                            requireActivity().contentResolver.openInputStream(uri)?.readBytes()
                                ?.let { bytes ->
                                    val op: String =
                                        temporaryLocation(requireContext()).absolutePath + getString(
                                            R.string.page_prefix
                                        ) + i.toString() + "_" + System.currentTimeMillis() + getString(
                                            R.string.page_extension
                                        )
                                    ImageToPdf(
                                        requireContext(),
                                        bytes,
                                        op,
                                        i,
                                        pageSize,
                                        object : ImageToPdfListener {
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
                                                renderingAdapter.data(pages)
                                            }
                                        })
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

                    when {
                        requireActivity().contentResolver.getType(uri) == TYPE_PDF -> {
                            requireActivity().contentResolver.openInputStream(uri)?.readBytes()?.let {
                                pageRenderer(it)
                            }
                        }
                        requireActivity().contentResolver.getType(uri) == TYPE_TXT -> {
                            requireActivity().contentResolver.openInputStream(uri)?.let { inputStream ->
                                val name = temporaryLocation(requireContext()).absolutePath + File.separator + timeStamp + getString(R.string.pdf_extension)
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
                        else -> Toast.makeText(requireContext(),"e",Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }

        directoryPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            if(result.resultCode == RESULT_OK){
                result?.data?.data?.also { uri ->
                    val outputDir = DocumentFile.fromTreeUri(requireContext(),uri)
                    outputDir?.let {
                        PdfToImage(requireContext(),timeStamp,outputDir, pages, object :
                            PdfToImageListener {
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

