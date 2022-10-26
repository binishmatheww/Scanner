package com.binishmatheww.scanner.views.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.binishmatheww.scanner.R
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File
import java.io.IOException


class OcrFragment : Fragment() {

    private lateinit var textInputEditText: TextInputEditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_ocr, container, false)
    }

    override fun onViewCreated(layout : View, savedInstanceState: Bundle?) {
        super.onViewCreated(layout, savedInstanceState)

        textInputEditText = layout.findViewById(R.id.mySamplePreview)
        val ocrOk: ImageView = layout.findViewById(R.id.ocrOk)
        ocrOk.setOnClickListener {
            textInputEditText.text?.let {
                val string: String = it.toString()
                val intent = Intent()
                intent.putExtra("extractedText", string)
                //setResult(RESULT_OK, intent)
                //finish()

                val bundle = Bundle()
                bundle.putString("extractedText",string)
                setFragmentResult("extractedText",bundle)
                fragmentManager?.popBackStackImmediate()
            }

        }
        val textRecognizer = TextRecognizer.Builder(requireContext()).build()
        if (!textRecognizer.isOperational) {

            Toast.makeText(requireContext(), "Unable to start the text recognizer ...", Toast.LENGTH_LONG).show()

        } else {

            arguments?.getString("filePath")?.let {
                ocRecognitionTask(it,textRecognizer)
            }

        }
    }

    private fun ocRecognitionTask(filePath: String,textRecognizer: TextRecognizer) = CoroutineScope(IO).launch{
        val stringBuilder = java.lang.StringBuilder()
        val fd: ParcelFileDescriptor

        withContext(Main){
            Toast.makeText(requireContext(), "Please wait....", Toast.LENGTH_SHORT).show()
        }

        try {
            fd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val page = renderer.openPage(0)
            val bitmap =
                Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val frame: Frame = Frame.Builder().setBitmap(bitmap).build()
            val items = textRecognizer.detect(frame)
            for (i in 0 until items.size()) {
                val item = items.valueAt(i)
                stringBuilder.append(item.value)
                stringBuilder.append("\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        withContext(Main){
            textInputEditText.setText(stringBuilder.toString())
            Toast.makeText(requireContext(), stringBuilder.length.toString() + " characters found.", Toast.LENGTH_SHORT).show()
        }
    }

}