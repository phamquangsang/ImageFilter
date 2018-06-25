package com.example.nhatpham.camerafilter

import android.arch.lifecycle.ViewModel

class PreviewViewModel : ViewModel() {

    val openPreviewEvent = SingleLiveEvent<String>()
}