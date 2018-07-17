package com.example.nhatpham.camerafilter.preview

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.example.nhatpham.camerafilter.models.Config
import com.example.nhatpham.camerafilter.utils.NONE_CONFIG

internal class VideoPreviewViewModel : ViewModel() {

    val showFiltersEvent = MutableLiveData<Boolean>()
    val showThumbnailEvent = MutableLiveData<Boolean>().apply { value = true }
    val currentConfigLiveData = MutableLiveData<Config>().apply { value = NONE_CONFIG }
}