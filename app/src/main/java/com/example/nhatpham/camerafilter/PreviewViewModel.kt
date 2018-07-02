package com.example.nhatpham.camerafilter

import android.arch.lifecycle.ViewModel

class PreviewViewModel : ViewModel() {

    val openPhotoPreviewEvent = SingleLiveEvent<String>()
    val openVideoPreviewEvent = SingleLiveEvent<String>()
}