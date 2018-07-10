package com.example.nhatpham.camerafilter

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class Config(val name: String,
                           @SerializedName("assets_image_name") private val assetFileName: String) : Parcelable {
    val value: String
        get() {
            return if (!assetFileName.isEmpty()) "@adjust lut $assetFileName" else ""
        }
}