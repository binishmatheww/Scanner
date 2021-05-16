package com.binishmatheww.scanner.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.OnDialogButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProgressDialog(private val title : String,private var progress : LiveData<Int>,private val max : Int,private val onDialogButtonClickListener: OnDialogButtonClickListener) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_progress, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        isCancelable = false

        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.RoundShapeTheme)

        val inflater = requireActivity().layoutInflater
        val layout : View = inflater.inflate(R.layout.dialog_progress, null)

        val progressTitle : TextView = layout.findViewById(R.id.progressTitle)
        progressTitle.text = title
        val progressBar : ProgressBar = layout.findViewById(R.id.progressBar)
        progressBar.max = max.toInt()
        progressBar.progress = 0
        progress.observe(requireActivity(), { t ->
            t?.let {
                if(t == max){
                    dismiss()
                }
                progressBar.progress = it
            }
        })
        val progressCancel : Button = layout.findViewById(R.id.progressCancel)
        progressCancel.setOnClickListener {
            dismiss()
            onDialogButtonClickListener.onDialogCancel()
        }

        builder.setView(layout)

        return builder.create()
    }


}