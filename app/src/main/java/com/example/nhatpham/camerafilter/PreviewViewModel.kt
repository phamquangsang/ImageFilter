package com.example.nhatpham.camerafilter

import android.arch.lifecycle.ViewModel
import android.net.Uri

class PreviewViewModel : ViewModel() {

    val openPhotoPreviewEvent = SingleLiveEvent<Uri>()
    val openVideoPreviewEvent = SingleLiveEvent<Uri>()

}