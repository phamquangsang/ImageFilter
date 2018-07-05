package com.example.nhatpham.camerafilter

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.collections.ArrayList

const val APP_NAME = "Mingle"

internal val DEFAULT_CONFIG = Config("None", "")

internal val EFFECT_CONFIGS = ArrayList<Config>().apply { add(DEFAULT_CONFIG) }

internal data class Config(val name: String,
                           @SerializedName("assets_image_name") private val assetFileName: String) {
    val value: String
    get() {
        return if (!assetFileName.isEmpty()) "@adjust lut $assetFileName" else ""
    }
}

//val EFFECT_CONFIGS = mapOf("" to "None", // ASCII art (字符画效果)
//        "@adjust lut A1.jpg" to "A1",
//        "@adjust lut A2.jpg" to "A2",
//        "@adjust lut A3.jpg" to "A3",
//        "@adjust lut A4.jpg" to "A4",
//        "@adjust lut A5.jpg" to "A5",
//        "@adjust lut A6.jpg" to "A6",
//        "@adjust lut A7.jpg" to "A7",
//        "@adjust lut A8.jpg" to "A8",
//        "@adjust lut A9.jpg" to "A9",
//        "@adjust lut B1.jpg" to "B1",
//        "@adjust lut B2.jpg" to "B2",
//        "@adjust lut B3.jpg" to "B3",
//        "@adjust lut B4.jpg" to "B4",
//        "@adjust lut B5.jpg" to "B5",
//        "@adjust lut B6.jpg" to "B6",
//        "@adjust lut B7.jpg" to "B7",
//        "@adjust lut B8.jpg" to "B8",
//        "@adjust lut B9.jpg" to "B9",
//        "@adjust lut C1.jpg" to "C1",
//        "@adjust lut C2.jpg" to "C2",
//        "@adjust lut C3.jpg" to "C3",
//        "@adjust lut C4.jpg" to "C4",
//        "@adjust lut C5.jpg" to "C5",
//        "@adjust lut C6.jpg" to "C6",
//        "@adjust lut C7.jpg" to "C7",
//        "@adjust lut C8.jpg" to "C8",
//        "@adjust lut C9.jpg" to "C9",
//        "@adjust lut D1.jpg" to "D1",
//        "@adjust lut D2.jpg" to "D2",
//        "@adjust lut D3.jpg" to "D3",
//        "@adjust lut D4.jpg" to "D4",
//        "@adjust lut D5.jpg" to "D5",
//        "@adjust lut D6.jpg" to "D6",
//        "@adjust lut D7.jpg" to "D7",
//        "@adjust lut D8.jpg" to "D8",
//        "@adjust lut D9.jpg" to "D9",
//        "@adjust lut E1.jpg" to "E1",
//        "@adjust lut E2.jpg" to "E2",
//        "@adjust lut E3.jpg" to "E3",
//        "@adjust lut E4.jpg" to "E4",
//        "@adjust lut E5.jpg" to "E5",
//        "@adjust lut E6.jpg" to "E6",
//        "@adjust lut E7.jpg" to "E7",
//        "@adjust lut E8.jpg" to "E8",
//        "@adjust lut E9.jpg" to "E9")

internal fun requestPermissions(activity: Activity, requestCode: Int, vararg permissions: String): Boolean {
    val notGrantedPermissions = ArrayList<String>()
    for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            notGrantedPermissions.add(permission)
        }
    }
    if (!notGrantedPermissions.isEmpty()) {
        val notGrantedPermissionsArray = arrayOf<String>()
        ActivityCompat.requestPermissions(activity, notGrantedPermissions.toArray(notGrantedPermissionsArray), requestCode)
        return true
    }
    return false
}

internal fun convertDpToPixel(context: Context, dp: Float): Int {
    val displayMetrics = context.resources.displayMetrics
    return Math.round(dp * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()))
}

internal fun convertPixelsToDp(context: Context, px: Float): Int {
    val displayMetrics = context.resources.displayMetrics
    return Math.round(px / (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()))
}

internal fun getThumbnail(context: Context, videoUri: Uri): Bitmap? {
    var bitmap: Bitmap? = null
    var mediaMetadataRetriever = MediaMetadataRetriever()
    try {
        mediaMetadataRetriever.setDataSource(context, videoUri)
        bitmap = mediaMetadataRetriever.frameAtTime
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        if (mediaMetadataRetriever != null)
            mediaMetadataRetriever.release()
    }
    return bitmap
}

internal fun getPath(): String {
    val path = "${Environment.getExternalStorageDirectory().absolutePath}/$APP_NAME"
    File(path).run {
        if (!exists())
            mkdirs()
    }
    return path
}

internal fun generateImageFileName() = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().time)}.jpg"

internal fun generateVideoFileName() = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().time)}.mp4"

internal fun isMediaStoreUri(uri: Uri?): Boolean {
    return (uri != null && ContentResolver.SCHEME_CONTENT == uri.scheme
            && MediaStore.AUTHORITY == uri.authority)
}

internal fun isVideoUri(uri: Uri?): Boolean = uri != null && uri.pathSegments.contains("video")

internal fun isMediaStoreVideoUri(uri: Uri?): Boolean = isMediaStoreUri(uri) && isVideoUri(uri)

internal fun isMediaStoreImageUri(uri: Uri?): Boolean = isMediaStoreUri(uri) && !isVideoUri(uri)

internal fun isFileUri(uri: Uri?): Boolean = uri != null && ContentResolver.SCHEME_FILE == uri.scheme