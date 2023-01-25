package com.binishmatheww.scanner.models

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import java.io.File

data class PdfFile(
    val uri: Uri? = null,
    val displayName: String? = null,
    val file: File? = null,
) : Parcelable{
    constructor(parcel: Parcel) : this(

        uri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Uri::class.java.classLoader, Uri::class.java)
        }
        else {
            parcel.readParcelable(Uri::class.java.classLoader)
        },
        displayName = parcel.readString(),
        file = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readSerializable(File::class.java.classLoader, File::class.java)
        }
        else {
            parcel.readSerializable() as File?
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(displayName)
        parcel.writeSerializable(file)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PdfFile> {
        override fun createFromParcel(parcel: Parcel): PdfFile {
            return PdfFile(parcel)
        }

        override fun newArray(size: Int): Array<PdfFile?> {
            return arrayOfNulls(size)
        }

    }


}
