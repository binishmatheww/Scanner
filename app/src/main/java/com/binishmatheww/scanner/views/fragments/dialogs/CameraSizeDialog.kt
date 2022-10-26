package com.binishmatheww.scanner.views.fragments.dialogs

import android.app.Dialog
import android.content.SharedPreferences
import android.hardware.Camera
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.adapters.CameraSizeAdapter
import com.binishmatheww.scanner.views.listeners.CameraIndexClickListener
import com.binishmatheww.scanner.views.listeners.OnDialogButtonClickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CameraSizeDialog(private val cameraSize : ArrayList<Camera.Size>,private val onDialogButtonClickListener: OnDialogButtonClickListener) : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_camera_size, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = MaterialAlertDialogBuilder(requireContext(), R.style.RoundShapeTheme)

        val inflater = requireActivity().layoutInflater
        val layout : View = inflater.inflate(R.layout.dialog_camera_size, null)

        val cameraSizeView : RecyclerView = layout.findViewById(R.id.cameraSizeView)

        val cameraSizeAdapter = CameraSizeAdapter(cameraSize, object : CameraIndexClickListener {
            override fun itemClicked(position: Int) {
                onDialogButtonClickListener.onDialogConfirm(position)
            }
        })
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(context)
        cameraSizeView.layoutManager = layoutManager
        cameraSizeView.adapter = cameraSizeAdapter

        builder.setView(layout)

        return builder.create()
    }

}