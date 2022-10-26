package com.binishmatheww.scanner.views.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.listeners.OnDialogButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch


class EditorExtraDialog(private val onDialogButtonClickListener: OnDialogButtonClickListener) : DialogFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_editor_extra, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.RoundShapeTheme)

        val inflater = requireActivity().layoutInflater
        val layout : View = inflater.inflate(R.layout.dialog_editor_extra, null)

        val addPdf : ImageView = layout.findViewById(R.id.addPdf)
        addPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("addPdf")
        }

        val addImages : ImageView = layout.findViewById(R.id.addImages)
        addImages.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("addImages")
        }

        val addText : ImageView = layout.findViewById(R.id.addText)
        addText.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("addText")
        }

        val exportPdf : ImageView = layout.findViewById(R.id.exportPdf)
        exportPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("exportPdf")
        }

        val splitPdf : ImageView = layout.findViewById(R.id.splitPdf)
        splitPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("splitPdf")
        }

        val pdfToImages : ImageView = layout.findViewById(R.id.pdfToImages)
        pdfToImages.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("pdfToImages")
        }

        val compressPdf : ImageView = layout.findViewById(R.id.compressPdf)
        compressPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("compressPdf")
        }

        val encryptPdf : ImageView = layout.findViewById(R.id.encryptPdf)
        encryptPdf.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm("encryptPdf")
        }

        builder.setView(layout)

        return builder.create()
    }


}