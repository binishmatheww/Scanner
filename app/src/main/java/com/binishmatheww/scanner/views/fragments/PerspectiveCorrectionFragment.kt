package com.binishmatheww.scanner.views.fragments

import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.common.utils.temporaryLocation
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class PerspectiveCorrectionFragment : Fragment() {

    private lateinit var imageToBeProcessedBitmap : Bitmap
    private lateinit var imageProcessedBitmap : Bitmap
    private lateinit var preview: ImageView
    private lateinit var pcButton: Button
    private lateinit var pcCButton: Button
    private lateinit var rPcButton: Button


    private lateinit var topLeftPointer: ImageView
    private lateinit var topRightPointer: ImageView
    private lateinit var bottomLeftPointer: ImageView
    private lateinit var bottomRightPointer: ImageView


    private var topLeftX = 0f
    private var topLeftY = 0f
    private var topRightX = 0f
    private var topRightY = 0f
    private var bottomLeftX = 0f
    private var bottomLeftY = 0f
    private var bottomRightX = 0f
    private var bottomRightY = 0f


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_perspective_correction, container, false)
    }

    override fun onViewCreated(layout: View, savedInstanceState: Bundle?) {
        super.onViewCreated(layout, savedInstanceState)
        imageToBeProcessedBitmap = BitmapFactory.decodeFile(arguments?.getString("imageToBeProcessed"))
        imageProcessedBitmap = imageToBeProcessedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val resultPath  = temporaryLocation(requireContext()).absolutePath + File.separator + requireContext().getString(R.string.image_prefix) + System.currentTimeMillis() + requireContext().getString(R.string.image_extension)

        pcButton = layout.findViewById(R.id.pcButton)
        pcCButton = layout.findViewById(R.id.pcCButton)
        rPcButton = layout.findViewById(R.id.rPcButton)
        preview = layout.findViewById(R.id.perspectivePreview)
        preview.setImageBitmap(imageProcessedBitmap)

        topLeftPointer = layout.findViewById(R.id.topLeftPointer)
        topRightPointer = layout.findViewById(R.id.topRightPointer)
        bottomLeftPointer = layout.findViewById(R.id.bottomLeftPointer)
        bottomRightPointer = layout.findViewById(R.id.bottomRightPointer)

        topLeftPointer.setOnTouchListener(tlp)
        topRightPointer.setOnTouchListener(trp)
        bottomLeftPointer.setOnTouchListener(blp)
        bottomRightPointer.setOnTouchListener(brp)

        resetImageBitmap()

        /*

        PerspectiveCorrection(imageProcessedBitmap, object : PerspectiveCorrectionListener {

            override fun onPostExecute(src: FloatArray) {


                try {
                    topLeftX = src[0]
                    topLeftY = src[1]
                    bottomLeftX = src[2]
                    bottomLeftY = src[3]
                    bottomRightX = src[4]
                    bottomRightY = src[5]
                    topRightX = src[6]
                    topRightY = src[7]
                    Log.wtf("co","$topLeftX $topLeftY $topRightX $topRightY $bottomLeftX $bottomLeftY $bottomRightX $bottomRightY")
                    draw()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        })

        */

        rPcButton.setOnClickListener {
            resetImageBitmap()
        }

        pcCButton.setOnClickListener {
            try {
                val processedImageFile = File(resultPath)
                processedImageFile.createNewFile()
                val os: OutputStream = BufferedOutputStream(FileOutputStream(processedImageFile))
                imageProcessedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
                os.close()
                preview.setImageBitmap(imageProcessedBitmap)
                val bundle = Bundle()
                bundle.putString("processedImage",processedImageFile.absolutePath)
                setFragmentResult("processedImage",bundle)
                requireActivity().supportFragmentManager.popBackStackImmediate()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        pcButton.setOnClickListener {
            val sourceCoordinates = floatArrayOf(topLeftX, topLeftY, bottomLeftX, bottomLeftY, bottomRightX, bottomRightY, topRightX, topRightY)
            val destWidth: Float = (topRightX - topLeftX).coerceAtMost(bottomRightX - bottomLeftX)
            val destHeight: Float = (bottomLeftY - topLeftY).coerceAtLeast(bottomRightY - topRightY)
            val destinationCoordinates = floatArrayOf(0f, 0f, 0f, destHeight, destWidth, destHeight, destWidth, 0f)
            imageProcessedBitmap = adjustPerspective(imageProcessedBitmap, sourceCoordinates, destinationCoordinates, destWidth, destHeight)
            preview.setImageBitmap(imageProcessedBitmap)
            resetCoordinates()
        }



    }
    val tlp = View.OnTouchListener { view, event ->

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                topLeftX = view.x - event.rawX
                topLeftY = view.y - event.rawY
                view.performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                view.animate().x(event.rawX + topLeftX).y(event.rawY + topLeftY).setDuration(0).start()
                draw()
            }
        }
        true
    }

    val trp = View.OnTouchListener { view, event ->

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                topRightX = view.x - event.rawX
                topRightY = view.y - event.rawY
                view.performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                view.animate().x(event.rawX + topRightX).y(event.rawY + topRightY).setDuration(0).start()
                draw()
            }
        }
        true
    }

    val blp = View.OnTouchListener { view, event ->

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                bottomLeftX = view.x - event.rawX
                bottomLeftY = view.y - event.rawY
                view.performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                view.animate().x(event.rawX + bottomLeftX).y(event.rawY + bottomLeftY).setDuration(0).start()
                draw()
            }
        }
        true
    }

    val brp = View.OnTouchListener { view, event ->

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                bottomRightX = view.x - event.rawX
                bottomRightY = view.y - event.rawY
                view.performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                view.animate().x(event.rawX + bottomRightX).y(event.rawY + bottomRightY).setDuration(0).start()
                draw()
            }
        }
        true
    }


    private fun resetImageBitmap(){
        imageProcessedBitmap = imageToBeProcessedBitmap
        preview.setImageBitmap(imageProcessedBitmap)
        resetCoordinates()
        draw()
    }

    private fun resetCoordinates(){
        val factor = 50f
        topLeftX = factor
        topLeftY = factor

        topRightX = imageProcessedBitmap.width.toFloat() - factor
        topRightY = factor

        bottomLeftX = factor
        bottomLeftY = imageProcessedBitmap.height.toFloat() - factor

        bottomRightX = imageProcessedBitmap.width.toFloat() - factor
        bottomRightY = imageProcessedBitmap.height.toFloat() - factor

        //draw()
    }

    private fun draw() {
        val previewImage = imageProcessedBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(previewImage)

        val linePaint = Paint()
        linePaint.color = Color.GREEN
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 7f
        linePaint.isAntiAlias = true

        val circlePaint = Paint()
        circlePaint.color = Color.GREEN
        circlePaint.style = Paint.Style.FILL
        circlePaint.isAntiAlias = true

        canvas.drawLine(topLeftX, topLeftY, topRightX, topRightY, linePaint)
        canvas.drawCircle(topLeftX,topLeftY,21f,circlePaint)

        canvas.drawLine(topRightX, topRightY, bottomRightX, bottomRightY, linePaint)
        canvas.drawCircle(topRightX,topRightY,21f,circlePaint)

        canvas.drawLine(bottomRightX, bottomRightY, bottomLeftX, bottomLeftY, linePaint)
        canvas.drawCircle(bottomRightX,bottomRightY,21f,circlePaint)

        canvas.drawLine(bottomLeftX, bottomLeftY, topLeftX, topLeftY, linePaint)
        canvas.drawCircle(bottomLeftX,bottomLeftY,21f,circlePaint)

        preview.setImageBitmap(previewImage)
    }

    private fun adjustPerspective(b: Bitmap, srcPoints: FloatArray, dstPoints: FloatArray, destWidth: Float, destHeight: Float): Bitmap {

        val w = if(destWidth.toInt()<1){
            1
        }
        else{
            destWidth.toInt()
        }
        
        val h = if (destHeight.toInt()<1){
            1
        }else{
            destHeight.toInt()
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val c = Canvas(result)
        val m = Matrix()
        m.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        c.drawColor(Color.WHITE)
        c.drawBitmap(b, m, p)

        //Log.wtf("src","${srcPoints[0]} ${srcPoints[1]} ${srcPoints[2]} ${srcPoints[3]} ${srcPoints[4]} ${srcPoints[5]} ${srcPoints[6]} ${srcPoints[7]}")
        //Log.wtf("dst","${dstPoints[0]} ${dstPoints[1]} ${dstPoints[2]} ${dstPoints[3]} ${dstPoints[4]} ${dstPoints[5]} ${dstPoints[6]} ${dstPoints[7]}")

        //Log.wtf("src-dst","${srcPoints[0]-dstPoints[0]} ${srcPoints[1]-dstPoints[1]} ${srcPoints[2]-dstPoints[2]} ${srcPoints[3]-dstPoints[3]} ${srcPoints[4]-dstPoints[4]} ${srcPoints[5]-dstPoints[5]} ${srcPoints[6]-dstPoints[6]} ${srcPoints[7]-dstPoints[7]}")

        Log.wtf("dst-src","${dstPoints[0]-srcPoints[0]} ${dstPoints[1]-srcPoints[1]} ${dstPoints[2]-srcPoints[2]} ${dstPoints[3]-srcPoints[3]} ${dstPoints[4]-srcPoints[4]} ${dstPoints[5]-srcPoints[5]} ${dstPoints[6]-srcPoints[6]} ${dstPoints[7]-srcPoints[7]}")

        return result
    }

}