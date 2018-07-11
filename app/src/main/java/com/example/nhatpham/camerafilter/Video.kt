package com.example.nhatpham.camerafilter

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
internal data class Video(val uri: Uri, val config: Config) : Parcelable