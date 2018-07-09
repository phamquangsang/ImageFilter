package com.example.nhatpham.camerafilter

import android.arch.lifecycle.ViewModel
import android.net.Uri

internal class MainViewModel : ViewModel() {

    val openPhotoPreviewEvent = SingleLiveEvent<Uri>()
    val openVideoPreviewEvent = SingleLiveEvent<Uri>()
    val openGalleryEvent = SingleLiveEvent<Void>()
    val doneEditEvent = SingleLiveEvent<Uri>()
}