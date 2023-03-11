package com.binishmatheww.scanner.models

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.core.net.toFile

@Suppress("DEPRECATION")
data class PdfFile(
    val uri: Uri,
    val name: String? = if(uri.scheme == "file") uri.toFile().name else null,
    val filterSeed: Float = 100f
) : Parcelable{
    constructor(parcel: Parcel) : this(
        uri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Uri::class.java.classLoader, Uri::class.java) ?: Uri.EMPTY
        }
        else {
            parcel.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY
        },
        name = parcel.readString(),
        filterSeed = parcel.readFloat(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(name)
        parcel.writeFloat(filterSeed)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun isFile(): Boolean = uri.scheme == "file"
    fun delete(): Boolean = if(uri.scheme == "file") uri.toFile().delete() else false

    companion object CREATOR : Parcelable.Creator<PdfFile> {
        override fun createFromParcel(parcel: Parcel): PdfFile {
            return PdfFile(parcel)
        }

        override fun newArray(size: Int): Array<PdfFile?> {
            return arrayOfNulls(size)
        }

    }


}
