package com.binishmatheww.scanner.views.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.os.AsyncTask
import com.binishmatheww.scanner.views.listeners.PerspectiveCorrectionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class PerspectiveCorrection(var imageToBeProcessed: Bitmap,var perspectiveListener: PerspectiveCorrectionListener) {


    init {

        CoroutineScope(IO).launch {
            val imageWidth = imageToBeProcessed.width
            val imageHeight = imageToBeProcessed.height
            var topLeftX: Float
            var topLeftY: Float
            var topRightX: Float
            var topRightY: Float
            var bottomLeftX: Float
            var bottomLeftY: Float
            var bottomRightX: Float
            var bottomRightY: Float
            topLeftX = 0.0f
            topLeftY = 0.0f
            topRightX = imageWidth.toFloat()
            topRightY = 0.0f
            bottomLeftX = 0.0f
            bottomLeftY = imageHeight.toFloat()
            bottomRightX = imageWidth.toFloat()
            bottomRightY = imageHeight.toFloat()
            var src = floatArrayOf(
                    topLeftX, topLeftY,
                    bottomLeftX, bottomLeftY,
                    bottomRightX, bottomRightY,
                    topRightX, topRightY
            )
            try {
                imageToBeProcessed = imageToBeProcessed.copy(Bitmap.Config.ARGB_8888, true)
                val min = 120
                val max = 250
                var pixel: Int
                var pixelR: Int
                var pixelG: Int
                var pixelB: Int
                for (y in imageHeight / 2 downTo 1) {
                    for (x in imageWidth / 2 downTo 1) {
                        pixel = imageToBeProcessed.getPixel(x, y)
                        pixelR = Color.red(pixel)
                        pixelG = Color.green(pixel)
                        pixelB = Color.blue(pixel)
                        if (!this.isActive) {
                            break
                        } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                                        pixelB - pixelR
                                ) <= 10
                        ) {
                            topLeftX = x.toFloat()
                            topLeftY = y.toFloat()
                            break
                        }
                    }
                }
                for (y in imageHeight / 2 downTo 1) {
                    for (x in imageWidth / 2 until imageWidth) {
                        pixel = imageToBeProcessed.getPixel(x, y)
                        pixelR = Color.red(pixel)
                        pixelG = Color.green(pixel)
                        pixelB = Color.blue(pixel)
                        if (!this.isActive) {
                            break
                        } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                                        pixelB - pixelR
                                ) <= 10
                        ) {
                            topRightX = x.toFloat()
                            topRightY = y.toFloat()
                            break
                        }
                    }
                }
                for (x in imageWidth / 2 downTo 1) {
                    for (y in imageHeight / 2 until imageHeight) {
                        pixel = imageToBeProcessed.getPixel(x, y)
                        pixelR = Color.red(pixel)
                        pixelG = Color.green(pixel)
                        pixelB = Color.blue(pixel)
                        if (!this.isActive) {
                            break
                        } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                                        pixelB - pixelR
                                ) <= 10
                        ) {
                            bottomLeftX = x.toFloat()
                            bottomLeftY = y.toFloat()
                            break
                        }
                    }
                }
                for (x in imageWidth / 2 until imageWidth) {
                    for (y in imageHeight / 2 until imageHeight) {
                        pixel = imageToBeProcessed.getPixel(x, y)
                        pixelR = Color.red(pixel)
                        pixelG = Color.green(pixel)
                        pixelB = Color.blue(pixel)
                        if (!this.isActive) {
                            break
                        } else if (pixelR in min..max && abs(pixelG - pixelR) <= 10 && abs(
                                        pixelB - pixelR
                                ) <= 10
                        ) {
                            bottomRightX = x.toFloat()
                            bottomRightY = y.toFloat()
                            break
                        }
                    }
                }
                src = floatArrayOf(
                        topLeftX, topLeftY,
                        bottomLeftX, bottomLeftY,
                        bottomRightX, bottomRightY,
                        topRightX, topRightY
                )
            } catch (e: Exception) {
                e.printStackTrace()
                perspectiveListener.onPostExecute(src)
            }
            perspectiveListener.onPostExecute(src)
        }
    }

}
