package com.example.nhatpham.camerafilter

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics

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