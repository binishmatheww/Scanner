package com.binishmatheww.scanner.views.adapters

import android.hardware.Camera
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.listeners.CameraIndexClickListener
import com.binishmatheww.scanner.views.utils.calculateMegaPixels
import java.util.ArrayList

class CameraSizeAdapter(supportedCameraSizes: ArrayList<Camera.Size>, listener : CameraIndexClickListener) : RecyclerView.Adapter<CameraSizeAdapter.CameraSizeHolder>() {


    private var camSizes: ArrayList<Camera.Size> = supportedCameraSizes
    private var cameraIndexClickListener: CameraIndexClickListener = listener


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraSizeHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.layout_camera_size, parent, false)
        return CameraSizeHolder(view)
    }

    override fun onBindViewHolder(holder: CameraSizeHolder, position: Int) {
        holder.cameraSizeItem.text = calculateMegaPixels(camSizes[position]).plus("(${camSizes[position].width} x ${camSizes[position].height})")
        holder.itemView.setOnClickListener { cameraIndexClickListener.itemClicked(position) }
    }

    override fun getItemCount(): Int {
        return camSizes.size
    }

    class CameraSizeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var cameraSizeItem: TextView = itemView.findViewById(R.id.cameraSizeItem)
    }
}