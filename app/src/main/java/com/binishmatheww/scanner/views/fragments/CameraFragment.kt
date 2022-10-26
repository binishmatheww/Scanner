package com.binishmatheww.scanner.views.fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.media.MediaScannerConnection
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.MutableLiveData
import com.binishmatheww.scanner.R
import com.binishmatheww.scanner.views.fragments.dialogs.CameraSizeDialog
import com.binishmatheww.scanner.views.fragments.dialogs.ProgressDialog
import com.binishmatheww.scanner.views.listeners.OnDialogButtonClickListener
import com.binishmatheww.scanner.views.utils.calculateMegaPixels
import com.binishmatheww.scanner.views.utils.getOptimalPreviewSize
import com.binishmatheww.scanner.views.utils.temporaryLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable
import java.util.*


class CameraFragment : Fragment() , View.OnClickListener {

    private val animationTask: Runnable by lazy {
        Runnable {
            overlay.background = Color.argb(150, 255, 255, 255).toDrawable()
            overlay.postDelayed({
                overlay.background = null
            }, 50L)
        }
    }

    private lateinit var overlay: View
    private lateinit var capture : ImageView
    private lateinit var flash: ImageView
    private lateinit var toggleFilter : ImageView
    private lateinit var camera: Camera
    private lateinit var pictureCount: TextView
    private lateinit var pictureThumbnail: ImageView
    private lateinit var switchCameraSize:TextView
    private lateinit var finishCapturing : ImageView

    private var images = ArrayList<File>()

    private lateinit var cameraPreview: FrameLayout
    private var supportedCameraSizes = ArrayList<Camera.Size>()
    private var selectedSize = 0
    private var filterKey = 1.35f
    private var filter = false
    private lateinit var sharedPreferences: SharedPreferences

    private var  dialog : DialogFragment? = null

    private var progressLiveData = MutableLiveData<Int>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(layout : View, savedInstanceState: Bundle?) {
        super.onViewCreated(layout, savedInstanceState)

        sharedPreferences = requireActivity().getSharedPreferences(getString(R.string.app_preferences), Context.MODE_PRIVATE)
        selectedSize = sharedPreferences.getInt(getString(R.string.camera_size_index), 0)
        capture = layout.findViewById(R.id.captureButton)
        flash = layout.findViewById(R.id.flash)
        cameraPreview = layout.findViewById(R.id.cameraPreview)
        overlay = layout.findViewById(R.id.overlay)
        pictureCount = layout.findViewById(R.id.pictureCount)
        pictureThumbnail = layout.findViewById(R.id.pictureThumbnail)
        switchCameraSize = layout.findViewById(R.id.switchCameraSize)
        toggleFilter = layout.findViewById(R.id.filterToggle)
        finishCapturing = layout.findViewById(R.id.finishCapturing)
        pictureCount.textSize = 20f
        switchCameraSize.textSize = 20f
        initialize()
        capture.setOnClickListener(this)
        switchCameraSize.setOnClickListener(this)
        flash.setOnClickListener(this)
        toggleFilter.setOnClickListener(this)
        finishCapturing.setOnClickListener(this)


    }


    internal class CameraPreview(context: Context?, private val camera: Camera) : SurfaceView(context), SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                camera.setPreviewDisplay(holder)
                camera.parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                camera.startPreview()
            } catch (e: java.lang.Exception) {
                //e.printStackTrace()
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            if (holder.surface == null) {
                return
            }
            try {
                camera.stopPreview()
            } catch (e: java.lang.Exception) {
                //e.printStackTrace()
            }
            try {
                camera.setPreviewDisplay(holder)
                camera.startPreview()
            } catch (e: java.lang.Exception) {
                //e.printStackTrace()
            }
        }

        init {
            val holder = holder
            holder.addCallback(this)
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
    }

    private fun initialize() {
        CoroutineScope(Main).launch {
            try {
                camera = getCameraInstance()
                val preview = CameraPreview(context, camera)
                cameraPreview.addView(preview)
                supportedCameraSizes = camera.getParameters().getSupportedPictureSizes() as ArrayList<Camera.Size>
                setPreviewSize()
            } catch (e: Exception) {
                //e.printStackTrace()
            }
        }
    }

    private fun setPreviewSize() {
        try {
            camera.stopPreview()
            val pm: Camera.Parameters = camera.parameters
            var width = pm.supportedPictureSizes[selectedSize].width.toFloat()
            var height = pm.supportedPictureSizes[selectedSize].height.toFloat()
            switchCameraSize.text = calculateMegaPixels(pm.supportedPictureSizes[selectedSize])
            pm.setPictureSize(width.toInt(), height.toInt())
            height *= resources.displayMetrics.widthPixels.toFloat() / width
            width = resources.displayMetrics.widthPixels.toFloat()
            val optimalPreviewSize = getOptimalPreviewSize(pm.supportedPreviewSizes, width.toInt(), height.toInt())
            optimalPreviewSize?.let {
                pm.setPreviewSize(it.width, it.height)
                width *= resources.displayMetrics.widthPixels.toFloat() / height
                height = resources.displayMetrics.widthPixels.toFloat()
                val lp = ConstraintLayout.LayoutParams(height.toInt(), width.toInt())
                if (lp.height > resources.displayMetrics.heightPixels) {
                    lp.width = (resources.displayMetrics.widthPixels.toFloat() / width * height).toInt()
                    lp.height = resources.displayMetrics.widthPixels
                }
                cameraPreview.layoutParams = lp
                pm.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                camera.setDisplayOrientation(90)
                pm.setRotation(0)
                camera.parameters = pm
                camera.startPreview()
            }
        } catch (e: Exception) {
           //e.printStackTrace()
        }
    }

    private fun getCameraInstance(): Camera {
        return Camera.open()
    }

    private val picture = PictureCallback { data, camera ->
        val pictureFile = File(temporaryLocation(requireContext()),  File.separator + getString(R.string.image_prefix) + System.currentTimeMillis() + getString(R.string.image_extension))
        MediaScannerConnection.scanFile(context, arrayOf(pictureFile.toString()), null) { path, uri -> camera.startPreview() }

            CoroutineScope(IO).launch {
                try {
                val fos = FileOutputStream(pictureFile)
                val options = BitmapFactory.Options()
                var bitmap = BitmapFactory.decodeByteArray(data,0,data.size,options)
                val matrix = Matrix()
                matrix.postRotate(90f)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    if(filter){
                        val canvas = Canvas(bitmap)
                        val paint = Paint()
                        //paint.setColorFilter(new LightingColorFilter(0xFFFFFFFF,0x00222222));
                        paint.colorFilter = ColorMatrixColorFilter(
                            ColorMatrix(
                                floatArrayOf(
                                    filterKey,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    filterKey,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    filterKey,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    0f,
                                    1f,
                                    0f
                                )
                            )
                        )
                        canvas.drawBitmap(bitmap, 0f, 0f, paint)
                    }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.close()
                    withContext(Main){
                        images.add(pictureFile)
                        pictureThumbnail.setImageBitmap(bitmap)
                        pictureCount.text = images.size.toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }

    override fun onPause() {
        super.onPause()
        camera.stopPreview()
    }

    override fun onResume() {
        super.onResume()
        //camera.startPreview()
        initialize()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera.release()
    }

    override fun onClick(view: View?) {

        when (view?.id) {

            R.id.captureButton -> {
                try {
                    camera.takePicture(null, null, picture)
                    cameraPreview.post(animationTask)
                } catch (r: RuntimeException) {
                    Toast.makeText(context, "Processing Image...", Toast.LENGTH_SHORT).show()
                    initialize()
                }
            }

            R.id.finishCapturing -> {
                if (images.isEmpty()) {
                    Toast.makeText(requireContext(), "No images taken", Toast.LENGTH_SHORT).show()
                }
                else {
                    requireActivity().supportFragmentManager.let {  sf ->

                        val bundle = Bundle()
                        bundle.putSerializable("pages", images as Serializable)

                        if (sf.findFragmentByTag("pdfEditor") != null) {
                            Log.wtf("Frag","exists")
                            setFragmentResult("images",bundle)
                            sf.popBackStackImmediate()
                        } else {
                            Log.wtf("Frag","doesn't exist")
                            val frag = PdfEditorFragment()
                            val b = Bundle()
                            b.putBundle("images",bundle)
                            frag.arguments = b
                            sf.popBackStack()
                            sf.beginTransaction().replace(R.id.fContainer,frag,"pdfEditor").addToBackStack("pdfEditor").commitAllowingStateLoss()
                        }

                    }
                }
            }

            R.id.switchCameraSize -> {

                 dialog = CameraSizeDialog(supportedCameraSizes,object :
                     OnDialogButtonClickListener {
                    override fun onDialogConfirm(result: Any) {
                        dialog?.dismiss()
                        selectedSize = result.toString().toInt()
                        val editor: Editor = sharedPreferences.edit()
                        editor.putInt(getString(R.string.camera_size_index), selectedSize)
                        editor.commit()
                        setPreviewSize()
                    }

                    override fun onDialogCancel() {

                    }

                })

                requireActivity().supportFragmentManager.let {
                    dialog?.show(it,"cameraSizeDialog")
                }

            }
            R.id.flash -> {

                if (requireActivity().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                    val cParameters = camera.parameters
                    if (camera.parameters.flashMode != Camera.Parameters.FLASH_MODE_TORCH) {
                        flash.setColorFilter(Color.GREEN)
                        cParameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                        camera.parameters = cParameters
                    } else {
                        cParameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
                        flash.clearColorFilter()
                        camera.parameters = cParameters
                    }

                } else {
                    Toast.makeText(context, "Your phone does not have flashlight support.", Toast.LENGTH_LONG).show()
                }

            }
            R.id.filterToggle -> {

                if (filter) {
                    filter = false
                    toggleFilter.clearColorFilter()
                } else {
                    filter = true
                    toggleFilter.setColorFilter(Color.GREEN)
                }

            }
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



}