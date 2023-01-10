package com.binishmatheww.scanner.common.utils

import android.util.Log
import okhttp3.internal.and

object ExifReader {
    private const val TAG = "CameraExif"

    // Returns the degrees in clockwise. Values are 0, 90, 180, or 270.
    fun getOrientation(jpeg: ByteArray?): Float {
        if (jpeg == null) {
            return 0f
        }
        var offset = 0
        var length = 0

        // ISO/IEC 10918-1:1993(E)
        while (offset + 3 < jpeg.size && jpeg[offset++] and 0xFF == 0xFF) {
            val marker: Int = jpeg[offset] and 0xFF

            // Check if the marker is a padding.
            if (marker == 0xFF) {
                continue
            }
            offset++

            // Check if the marker is SOI or TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue
            }
            // Check if the marker is EOI or SOS.
            if (marker == 0xD9 || marker == 0xDA) {
                break
            }

            // Get the length and check if it is reasonable.
            length = pack(jpeg, offset, 2, false)
            if (length < 2 || offset + length > jpeg.size) {
                Log.e(TAG, "Invalid length")
                return 0f
            }

            // Break if the marker is EXIF in APP1.
            if (marker == 0xE1 && length >= 8 && pack(
                    jpeg,
                    offset + 2,
                    4,
                    false
                ) == 0x45786966 && pack(jpeg, offset + 6, 2, false) == 0
            ) {
                offset += 8
                length -= 8
                break
            }

            // Skip other markers.
            offset += length
            length = 0
        }

        // JEITA CP-3451 Exif Version 2.2
        if (length > 8) {
            // Identify the byte order.
            var tag = pack(jpeg, offset, 4, false)
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                Log.e(TAG, "Invalid byte order")
                return 0f
            }
            val littleEndian = tag == 0x49492A00

            // Get the offset and check if it is reasonable.
            var count = pack(jpeg, offset + 4, 4, littleEndian) + 2
            if (count < 10 || count > length) {
                Log.e(TAG, "Invalid offset")
                return 0f
            }
            offset += count
            length -= count

            // Get the count and go through all the elements.
            count = pack(jpeg, offset - 2, 2, littleEndian)
            while (count-- > 0 && length >= 12) {
                // Get the tag and check if it is orientation.
                tag = pack(jpeg, offset, 2, littleEndian)
                if (tag == 0x0112) {
                    // We do not really care about type and count, do we?
                    val orientation = pack(jpeg, offset + 8, 2, littleEndian)
                    when (orientation) {
                        1 -> return 0f
                        3 -> return 180f
                        6 -> return 90f
                        8 -> return 270f
                    }
                    Log.i(TAG, "Unsupported orientation")
                    return 0f
                }
                offset += 12
                length -= 12
            }
        }
        Log.i(TAG, "Orientation not found")
        return 0f
    }

    private fun pack(
        bytes: ByteArray, offset: Int, length: Int,
        littleEndian: Boolean
    ): Int {
        var offset = offset
        var length = length
        var step = 1
        if (littleEndian) {
            offset += length - 1
            step = -1
        }
        var value = 0
        while (length-- > 0) {
            value = value shl 8 or (bytes[offset] and 0xFF)
            offset += step
        }
        return value
    }
}