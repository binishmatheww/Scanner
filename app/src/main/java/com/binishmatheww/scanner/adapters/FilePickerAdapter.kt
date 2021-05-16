package com.binishmatheww.scanner.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.ThumbnailUtils
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.listeners.OnFileClickListener
import com.binishmatheww.scanner.utils.getResizedBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream


class FilePickerAdapter(private var context: Context,private var files: ArrayList<File>, var onFileClickListener: OnFileClickListener) : RecyclerView.Adapter<FilePickerAdapter.FileViewHolder>() {

    fun data(files: ArrayList<File>){
        this.files = files
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view : View = LayoutInflater.from(parent.context).inflate(R.layout.layout_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {

        holder.fileName.text = files[position].name
        holder.fileThumbNail.setImageBitmap(null)
        holder.itemView.setOnClickListener {
            onFileClickListener.onClicked(position)
        }
        holder.fileDownload.setOnClickListener {
            onFileClickListener.onDownload(position)
        }

        CoroutineScope(IO).launch {

            val f = File(context.cacheDir, (files[position].name).plus(context.getString(R.string.png_extension)))
            if(f.exists()){
                val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(f.absolutePath), 100, 100)
                withContext(Main){
                    bitmap?.let {
                        holder.fileThumbNail.setImageBitmap(it)
                    }
                }
            }
            else{
                if (files[position].name.endsWith(context.getString(R.string.jpeg_extension))) {
                     try {
                        val parcelFileDescriptor: ParcelFileDescriptor? = ParcelFileDescriptor.open(files[position],ParcelFileDescriptor.MODE_READ_ONLY)
                        val fileDescriptor: FileDescriptor? = parcelFileDescriptor?.fileDescriptor
                        val bitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFileDescriptor(fileDescriptor), 100, 100)
                         withContext(Main){
                             bitmap?.let {
                                 holder.fileThumbNail.setImageBitmap(it)
                             }
                         }
                        parcelFileDescriptor?.close()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (files[position].name.endsWith(context.getString(R.string.pdf_extension))) {
                    try {
                        val renderer: PdfRenderer
                        val fd: ParcelFileDescriptor? = ParcelFileDescriptor.open(files[position],ParcelFileDescriptor.MODE_READ_ONLY)
                        fd?.let { it ->
                            renderer = PdfRenderer(it)
                            val page: PdfRenderer.Page = renderer.openPage(0)
                            val bitmap = getResizedBitmap(Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888), 100)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            renderer.close()
                            withContext(Main){
                                    holder.fileThumbNail.setImageBitmap(bitmap)
                            }
                            f.createNewFile()
                            val out = FileOutputStream(f)
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            out.close()
                        }
                    } catch (e: Exception) {
                        Log.wtf("FilePickerAdapter","${files[position].name} ${e.message}")
                    }
                }
            }
            }



    }


    override fun getItemCount(): Int {
        return files.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }


    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileThumbNail : ImageView = itemView.findViewById(R.id.fileThumbNail)
        val fileName : TextView = itemView.findViewById(R.id.fileName)
        val fileDownload : ImageView = itemView.findViewById(R.id.fileDownload)
    }

}