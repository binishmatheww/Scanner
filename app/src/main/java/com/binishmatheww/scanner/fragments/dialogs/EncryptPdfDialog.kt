package com.binishmatheww.scanner.fragments.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.OnDialogButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class EncryptPdfDialog(private val onDialogButtonClickListener: OnDialogButtonClickListener) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_encrypt_pdf, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.RoundShapeTheme)
        val inflater = requireActivity().layoutInflater
        val layout : View = inflater.inflate(R.layout.dialog_encrypt_pdf, null)

        val encryptPassword : EditText = layout.findViewById(R.id.encryptPassword)
        val encryptButton : Button = layout.findViewById(R.id.encryptButton)
        encryptButton.setOnClickListener {
            onDialogButtonClickListener.onDialogConfirm(encryptPassword.text.toString())
        }

        builder.setView(layout)

        return builder.create()
    }

}