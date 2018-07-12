package com.example.nhatpham.camerafilter.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.example.nhatpham.camerafilter.models.Config
import com.example.nhatpham.camerafilter.utils.*
import kotlinx.android.parcel.Parcelize

internal data class Video(val uri: Uri,
                          val config: Config,
                          val source: Source = Source.NONE) : KParcelable {

    constructor(p : Parcel) : this(uri = p.readTypedObjectCompat(Uri.CREATOR)!!,
            config = p.readTypedObjectCompat(Config.CREATOR)!!, source = p.readEnum<Source>()!!)

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeTypedObjectCompat(uri, flags)
        writeTypedObjectCompat(config, flags)
        writeEnum(source)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Video)
    }
}

internal fun Video.isFromCamera() = source == Source.CAMERA

internal fun Video.isFromGallery() = source == Source.GALLERY