package com.example.nhatpham.camerafilter.camera

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.databinding.ObservableBoolean
import com.example.nhatpham.camerafilter.Config
import com.example.nhatpham.camerafilter.DEFAULT_CONFIG
import com.example.nhatpham.camerafilter.SingleLiveEvent

internal class CameraViewModel : ViewModel() {

    val showFiltersEvent = MutableLiveData<Boolean>()
    val currentModeLiveData = MutableLiveData<CameraMode>().apply { value = CameraMode.Photo }
    val currentConfigLiveData = MutableLiveData<Config>().apply { value = DEFAULT_CONFIG }
    val recordingStateLiveData = SingleLiveEvent<Boolean>()

    val isRecording = ObservableBoolean(false)
}