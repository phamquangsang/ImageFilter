package com.example.nhatpham.camerafilter

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean

internal class CameraViewModel : ViewModel() {

    val showFiltersEvent = MutableLiveData<Boolean>()
    val currentModeLiveData = MutableLiveData<String>().apply { value = "Photo" }
    val currentConfigLiveData = MutableLiveData<Config>().apply { value = DEFAULT_CONFIG }
    val recordingStateLiveData = SingleLiveEvent<Boolean>()

    val isRecording = ObservableBoolean(false)
}