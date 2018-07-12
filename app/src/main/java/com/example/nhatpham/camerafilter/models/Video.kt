package com.example.nhatpham.camerafilter.models

import android.net.Uri
import android.os.Parcelable
import com.example.nhatpham.camerafilter.models.Config
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class Video(val uri: Uri, val config: Config) : Parcelable