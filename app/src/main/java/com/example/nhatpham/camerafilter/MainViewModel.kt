package com.example.nhatpham.camerafilter

import android.arch.lifecycle.ViewModel
import android.net.Uri

internal class MainViewModel : ViewModel() {

    val openPhotoPreviewEvent = SingleLiveEvent<Photo>()
    val openPhotoPreviewFromCameraEvent = SingleLiveEvent<Photo>()
    val openVideoPreviewEvent = SingleLiveEvent<Video>()
    val openVideoPreviewFromCameraEvent = SingleLiveEvent<Video>()
    val openGalleryEvent = SingleLiveEvent<Void>()
    val doneEditEvent = SingleLiveEvent<Uri>()
}