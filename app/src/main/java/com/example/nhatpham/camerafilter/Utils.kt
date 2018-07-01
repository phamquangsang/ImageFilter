package com.example.nhatpham.camerafilter

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics

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