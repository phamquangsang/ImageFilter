package com.example.nhatpham.camerafilter

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.provider.MediaStore
import android.content.ContentValues
import android.content.ContentResolver
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.*
import android.content.ContentUris
import android.graphics.Matrix
import android.os.Environment
import org.wysaid.myUtils.ImageUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

const val APP_NAME = "Mingle"

val EFFECT_CONFIGS = mapOf("" to "None", // ASCII art (字符画效果)
        "@adjust lut A1.jpg" to "A1",
        "@adjust lut A2.jpg" to "A2",
        "@adjust lut A3.jpg" to "A3",
        "@adjust lut A4.jpg" to "A4",
        "@adjust lut A5.jpg" to "A5",
        "@adjust lut A6.jpg" to "A6",
        "@adjust lut A7.jpg" to "A7",
        "@adjust lut A8.jpg" to "A8",
        "@adjust lut A9.jpg" to "A9",
        "@adjust lut B1.jpg" to "B1",
        "@adjust lut B2.jpg" to "B2",
        "@adjust lut B3.jpg" to "B3",
        "@adjust lut B4.jpg" to "B4",
        "@adjust lut B5.jpg" to "B5",
        "@adjust lut B6.jpg" to "B6",
        "@adjust lut B7.jpg" to "B7",
        "@adjust lut B8.jpg" to "B8",
        "@adjust lut B9.jpg" to "B9")

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

internal fun convertDpToPixel(context: Context, dp: Float) : Int {
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
        if(!exists())
            mkdirs()
    }
    return path
}

internal fun generateImageFileName() = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().time)}.jpg"

internal fun generateVideoFileName() = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().time)}.mp4"