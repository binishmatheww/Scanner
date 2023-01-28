package com.binishmatheww.scanner.models

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable

@Suppress("DEPRECATION")
data class PdfFile(
    val uri: Uri? = null,
    val displayName: String? = null,
) : Parcelable{
    constructor(parcel: Parcel) : this(

        uri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            parcel.readParcelable(Uri::class.java.classLoader, Uri::class.java)
        }
        else {
            parcel.readParcelable(Uri::class.java.classLoader)
        },
        displayName = parcel.readString(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(displayName)
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
