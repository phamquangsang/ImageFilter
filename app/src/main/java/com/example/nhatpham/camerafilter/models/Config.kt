package com.example.nhatpham.camerafilter.models

import android.os.Parcel
import com.example.nhatpham.camerafilter.utils.KParcelable
import com.example.nhatpham.camerafilter.utils.parcelableCreator
import com.google.gson.annotations.SerializedName

internal data class Config(val name: String,
                           @SerializedName("assets_image_name") private val assetFileName: String) : KParcelable {

    constructor(p : Parcel) : this(name = p.readString(), assetFileName = p.readString())

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeString(assetFileName)
    }

    val value: String
        get() {
            return if (!assetFileName.isEmpty()) "@adjust lut $assetFileName" else ""
        }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Config)
    }
}