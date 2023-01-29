package com.binishmatheww.scanner.views.adapters


import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.utils.getContrastBrightnessFilter
import com.binishmatheww.scanner.common.utils.vibrate
import com.binishmatheww.scanner.views.listeners.PageClickListener
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class PageAdapter(var context: Context,var pages: ArrayList<File>,var pageClickListener: PageClickListener) : RecyclerView.Adapter<PageAdapter.PageHolder>() {

    private var editMode = false
    private var key = 0f
    private val brightness = 0f

    fun data(pages: ArrayList<File>){
        this.pages = pages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.layout_page, parent, false)
        return PageHolder(view)
    }

    override fun onBindViewHolder(holder: PageHolder, position: Int) {
            if(!editMode){
                holder.options.visibility = View.INVISIBLE
            }
            else{
                holder.options.visibility = View.VISIBLE
            }
            CoroutineScope(Default).launch {
                var bitmap: Bitmap? = null
                try {
                    val renderer: PdfRenderer
                    val fd: ParcelFileDescriptor
                    val file = File(pages[position].absolutePath)
                    fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(fd)
                    val page: PdfRenderer.Page = renderer.openPage(0)
                    bitmap = Bitmap.createBitmap(page.width , page.height , Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    canvas.drawBitmap(bitmap, 0.0f, 0.0f, null)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    //bitmap = getResizedBitmap(bitmap, 1000)
                    page.close()
                    renderer.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Main) {
                    Glide.with(context).load(bitmap).into(holder.page)
                    //holder.pageNo.text = context.getString(R.string.page_prefix).plus((position + 1).toString())

                    holder.page.setOnClickListener {
                        //pageClickListener.pageClicked(position)
                    }

                    holder.page.setOnLongClickListener{
                        context.vibrate()
                        if (holder.options.visibility == View.INVISIBLE) {
                            holder.options.visibility = View.VISIBLE
                            pageClickListener.pageClicked(position)
                            editMode = true
                            notifyDataSetChanged()
                        } else {
                            holder.options.visibility = View.INVISIBLE
                            editMode = false
                            notifyDataSetChanged()
                        }
                        return@setOnLongClickListener false
                    }
                    holder.rotateClockWise.setOnClickListener{
                        pageClickListener.rotate(position, 90)
                    }
                    holder.rotateAntiClockWise.setOnClickListener{
                        pageClickListener.rotate(position, 270)
                    }
                    holder.crop.setOnClickListener{
                        pageClickListener.crop(position)
                    }
                    holder.ocr.setOnClickListener{
                        pageClickListener.ocr(position)
                    }
                    holder.apply.setOnClickListener{
                        if (key != 0f) {
                            pageClickListener.filter(position, key)
                            holder.page.clearColorFilter()
                            holder.slider.progress = 100
                            }
                    }
                    holder.up.setOnClickListener{
                        pageClickListener.up(position)
                    }
                    holder.down.setOnClickListener{
                        pageClickListener.down(position)
                    }

                    holder.deletePage.setOnClickListener{
                        pageClickListener.deletePage(position)
                    }


                    holder.slider.max = 200
                    holder.slider.progress = 100
                    holder.slider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                        override fun onProgressChanged(slider: SeekBar, progress: Int, b: Boolean) {
                            key = progress / 100f
                            holder.page.clearColorFilter()
                            val cm = ColorMatrix(floatArrayOf(
                                    key, 0f, 0f, 0f, brightness, 0f, key, 0f, 0f, brightness, 0f, 0f, key, 0f, brightness, 0f, 0f, 0f, 1f, brightness
                            ))
                            holder.page.colorFilter = ColorMatrixColorFilter(cm)
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar) {

                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar) {
                            holder.page.clearColorFilter()
                            holder.page.colorFilter = getContrastBrightnessFilter(key, 0f)
                        }
                    })
                }
            }
        }



        class PageHolder(pageLayout: View) : RecyclerView.ViewHolder(pageLayout) {
            var page : ImageView = pageLayout.findViewById(R.id.pagePreview)
            var pageNo  : TextView = pageLayout.findViewById(R.id.pageNumber)
            var options  : ConstraintLayout = pageLayout.findViewById(R.id.options)
            var rotateClockWise  : ImageView = pageLayout.findViewById(R.id.rotateclockwise)
            var rotateAntiClockWise  : ImageView = pageLayout.findViewById(R.id.rotateanticlockwise)
            var up  : ImageView = pageLayout.findViewById(R.id.up)
            var down  : ImageView = pageLayout.findViewById(R.id.down)
            var crop  : Button = pageLayout.findViewById(R.id.crop)
            var ocr  : Button = pageLayout.findViewById(R.id.ocrfrompage)
            var apply  : Button = pageLayout.findViewById(R.id.apply)
            var slider  : SeekBar = pageLayout.findViewById(R.id.slider)
            var deletePage  : TextView = pageLayout.findViewById(R.id.deletepage)
        }




    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemCount(): Int {
        return pages.size
    }

}
