package com.example.nhatpham.camerafilter

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.models.Photo
import com.example.nhatpham.camerafilter.models.Source
import com.example.nhatpham.camerafilter.models.Video
import com.example.nhatpham.camerafilter.utils.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.lang.Exception
import java.net.URL

internal class MainViewModel(application: Application) : AndroidViewModel(application) {

    val openPhotoPreviewEvent = SingleLiveEvent<Photo>()
    val openVideoPreviewEvent = SingleLiveEvent<Video>()
    val openGalleryEvent = SingleLiveEvent<Void>()
    val doneEditEvent = SingleLiveEvent<Uri>()

    internal fun checkToOpenPreview(mediaUri: Uri) {
        doAsync {
            when {
                isImageUri(mediaUri) -> openPhotoPreviewEvent.postValue(Photo(mediaUri, NONE_CONFIG, Source.NONE))
                isVideoUri(mediaUri) -> openVideoPreviewEvent.postValue(Video(mediaUri, NONE_CONFIG, Source.NONE))
                else -> doneEditEvent.postValue(null)
            }
        }
    }

    private fun isImageUri(uri: Uri): Boolean {
        return when {
            isMediaStoreUri(uri) -> ofImage().any { it.checkType(getApplication(), uri) }
            isFileUri(uri) -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
                    .startsWith("image")
            else -> {
                val url = uri.toString()
                return if (URLUtil.isValidUrl(url) && (URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url))) {
                    isCachedAvailable(url) || isValidContentType(url, "image")
                } else false
            }
        }
    }

    private fun isCachedAvailable(url: String): Boolean {
        return try {
            Glide.with(getApplication<Application>())
                    .load(url)
                    .apply(RequestOptions().onlyRetrieveFromCache(true))
                    .submit()
                    .get() != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isValidContentType(url: String, type: String): Boolean {
        return try {
            if (isNetworkConnected(getApplication())) {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 1000
                    readTimeout = 1000
                }
                connection.getHeaderField("Content-Type")?.startsWith(type) ?: false
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isVideoUri(uri: Uri): Boolean {
        return when {
            isMediaStoreUri(uri) -> ofVideo().any { it.checkType(getApplication(), uri) }
            isFileUri(uri) -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
                    .startsWith("video")
            else -> false
        }
    }
}