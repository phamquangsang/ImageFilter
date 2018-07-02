package com.example.nhatpham.camerafilter

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics

val EFFECT_CONFIGS = arrayOf("", // ASCII art (字符画效果)
        "@adjust lut A.jpg",
        "@adjust lut A-2.jpg",
        "@adjust lut A-3.jpg",
        "@adjust lut A-4.jpg",
        "@adjust lut A-5.jpg",
        "@adjust lut A-6.jpg",
        "@adjust lut A-7.jpg",
        "@adjust lut A-8.jpg",
        "@adjust lut A-9.jpg",
        "@adjust lut A-10.jpg",
        "@adjust lut A-11.jpg",
        "@adjust lut A-12.jpg",
        "@adjust lut A-13.jpg",
        "@adjust lut A-14.jpg",
        "@adjust lut A-15.jpg",
        "@adjust lut lookup_amatorka.png",
        "@adjust lut lookup_fgfacolor.png",
        "@adjust lut lookup_lofi.png",
        "@adjust lut lookup_mayfair.png",
        "@adjust lut lookup_miss_etikate.png",
        "@adjust lut lookup_nashville.png",
        "@adjust lut lookup_nguocnang.png",
        "@adjust lut lookup_soft_elegance_1.png",
        "@adjust lut lookup_soft_elegance_2.png")

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