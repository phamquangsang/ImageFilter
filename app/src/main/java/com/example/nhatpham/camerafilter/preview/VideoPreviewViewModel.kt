package com.example.nhatpham.camerafilter.preview

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.example.nhatpham.camerafilter.NONE_CONFIG
import com.example.nhatpham.camerafilter.models.Config

internal class VideoPreviewViewModel : ViewModel() {

    val showFiltersEvent = MutableLiveData<Boolean>()
    val currentConfigLiveData = MutableLiveData<Config>().apply { value = NONE_CONFIG }
}