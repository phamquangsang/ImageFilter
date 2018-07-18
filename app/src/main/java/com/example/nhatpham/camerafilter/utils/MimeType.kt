package com.example.nhatpham.camerafilter.utils

import android.content.Context
import android.net.Uri
import android.support.v4.util.ArraySet
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.util.*

internal enum class MimeType constructor(private val mMimeTypeName: String,
                                         private val mExtensions: Set<String>) {

    // ============== images ==============
    JPEG("image/jpeg", arraySetOf(
            "jpg",
            "jpeg"
    )),
    PNG("image/png", arraySetOf(
            "png"
    )),
    GIF("image/gif", arraySetOf(
            "gif"
    )),
    BMP("image/x-ms-bmp", arraySetOf(
            "bmp"
    )),
    WEBP("image/webp", arraySetOf(
            "webp"
    )),

    // ============== videos ==============
    MPEG("video/mpeg", arraySetOf(
            "mpeg",
            "mpg"
    )),
    MP4("video/mp4", arraySetOf(
            "mp4",
            "m4v"
    )),
    QUICKTIME("video/quicktime", arraySetOf(
            "mov"
    )),
    THREEGPP("video/3gpp", arraySetOf(
            "3gp",
            "3gpp"
    )),
    THREEGPP2("video/3gpp2", arraySetOf(
            "3g2",
            "3gpp2"
    )),
    MKV("video/x-matroska", arraySetOf(
            "mkv"
    )),
    WEBM("video/webm", arraySetOf(
            "webm"
    )),
    TS("video/mp2ts", arraySetOf(
            "ts"
    )),
    AVI("video/avi", arraySetOf(
            "avi"
    ));

    override fun toString(): String {
        return mMimeTypeName
    }

    fun checkType(context: Context, uri: Uri?): Boolean {
        val map = MimeTypeMap.getSingleton()
        if (uri == null) {
            return false
        }
        val type = map.getExtensionFromMimeType(context.contentResolver.getType(uri))
        var path: String? = null
        // lazy load the path and prevent resolve for multiple times
        var pathParsed = false
        for (extension in mExtensions) {
            if (extension == type) {
                return true
            }
            if (!pathParsed) {
                // we only resolve the path for one time
                path = getPathFromMediaUri(context, uri)
                if (!TextUtils.isEmpty(path)) {
                    path = path!!.toLowerCase(Locale.US)
                }
                pathParsed = true
            }
            if (path != null && path.endsWith(extension)) {
                return true
            }
        }
        return false
    }
}

internal fun ofAll(): Set<MimeType> {
    return EnumSet.allOf(MimeType::class.java)
}

internal fun of(type: MimeType, vararg rest: MimeType): Set<MimeType> {
    return EnumSet.of(type, *rest)
}

internal fun ofImage(): Set<MimeType> {
    return EnumSet.of(MimeType.JPEG, MimeType.PNG, MimeType.GIF, MimeType.BMP, MimeType.WEBP)
}

internal fun ofVideo(): Set<MimeType> {
    return EnumSet.of(MimeType.MPEG, MimeType.MP4, MimeType.QUICKTIME, MimeType.THREEGPP, MimeType.THREEGPP2, MimeType.MKV, MimeType.WEBM, MimeType.TS, MimeType.AVI)
}

internal fun arraySetOf(vararg suffixes: String): Set<String> {
    return ArraySet(Arrays.asList(*suffixes))
}

internal fun checkUriMimeType(context: Context, uri: Uri, mimeTypes: Set<MimeType>): Boolean {
    for (mimeType in mimeTypes) {
        if (mimeType.checkType(context, uri)) {
            return true
        }
    }
    return false
}