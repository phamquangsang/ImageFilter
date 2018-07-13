package com.example.nhatpham.camerafilter.camera

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.content.Context
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PagerSnapHelper
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.nhatpham.camerafilter.*
import com.example.nhatpham.camerafilter.databinding.FragmentCameraFiltersBinding
import com.example.nhatpham.camerafilter.models.*
import com.example.nhatpham.camerafilter.utils.*
import org.wysaid.camera.CameraInstance
import org.wysaid.myUtils.FileUtil
import org.wysaid.myUtils.ImageUtil
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class CameraFragment : Fragment() {

    private lateinit var mBinding: FragmentCameraFiltersBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var previewFiltersAdapter: PreviewFiltersAdapter
    private lateinit var modesAdapter: ModesAdapter

    private val mainHandler = Handler()
    private val snapHelper = PagerSnapHelper()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var timeRecordingFuture: ScheduledFuture<*>? = null
    private val currentConfig
        get() = cameraViewModel.currentConfigLiveData.value ?: NONE_CONFIG

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera_filters, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mainViewModel = getViewModel(activity!!)
        cameraViewModel = getViewModel(this)
        mBinding.cameraviewmodel = cameraViewModel

        cameraViewModel.showFiltersEvent.observe(viewLifecycleOwner, Observer { active ->
            showFilters(active ?: false)
        })

        cameraViewModel.currentModeLiveData.observe(viewLifecycleOwner, Observer {
            updateModeView(it ?: CameraMode.Photo)
        })

        if(cameraViewModel.cameraBackForwardLiveData.value == null) {
            cameraViewModel.cameraBackForwardLiveData.value = mBinding.cameraView.isCameraBackForward
        }
        cameraViewModel.cameraBackForwardLiveData.observe(viewLifecycleOwner, Observer {
            if(mBinding.cameraView.isCameraBackForward != it) {
                mBinding.cameraView.switchCamera()
            }
        })

        cameraViewModel.currentConfigLiveData.observe(viewLifecycleOwner, Observer { newConfig ->
            if (newConfig != null) {
                mBinding.cameraView.setFilterWithConfig(newConfig.value)
                mBinding.tvFilterName.text = newConfig.name
                previewFiltersAdapter.setNewConfig(newConfig)
            }
        })

        cameraViewModel.recordingStateLiveData.observe(viewLifecycleOwner, Observer {
            val active = it == true
            mBinding.btnRecord.setImageResource(if (active) R.drawable.stop_recording else R.drawable.start_record)
            cameraViewModel.isRecording.set(active)
            if (active)
                showFilters(false)
            else cameraViewModel.showFiltersEvent.value = cameraViewModel.showFiltersEvent.value
        })

        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewFiltersAdapter = PreviewFiltersAdapter(context!!, EFFECT_CONFIGS, object : PreviewFiltersAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: Config) {
                cameraViewModel.currentConfigLiveData.value = selectedConfig
            }
        })
        previewFiltersAdapter.imageUri = ""
        mBinding.rcImgPreview.adapter = previewFiltersAdapter

        mBinding.rcModes.layoutManager = ScaledCenterLayoutManager(context!!, LinearLayoutManager.HORIZONTAL, false)
        snapHelper.attachToRecyclerView(mBinding.rcModes)
        modesAdapter = ModesAdapter(arrayListOf(CameraMode.Photo, CameraMode.Video), object : ModesAdapter.OnItemInteractListener {
            override fun onModeSelected(mode: CameraMode, position: Int) {
                cameraViewModel.currentModeLiveData.value = mode
                mBinding.rcModes.smoothScrollToPosition(position)
            }
        })
        mBinding.rcModes.adapter = modesAdapter
        mBinding.rcModes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState != RecyclerView.SCROLL_STATE_IDLE)
                    return
                val layoutManager = recyclerView.layoutManager
                if (layoutManager is LinearLayoutManager) {
                    val snapView = snapHelper.findSnapView(layoutManager)
                    if (snapView != null) {
                        val pos = layoutManager.getPosition(snapView)
                        if (pos != RecyclerView.NO_POSITION)
                            cameraViewModel.currentModeLiveData.value = modesAdapter.getItem(pos)
                    }
                }
            }
        })

        mBinding.btnRecord.setOnClickListener(RecordListener())

        mBinding.btnTakePhoto.setOnClickListener {
            context?.let { takePhoto(it) }
        }

        mBinding.btnPickFilters.setOnClickListener {
            cameraViewModel.showFiltersEvent.value = cameraViewModel.showFiltersEvent.value?.not() ?: true
        }

        mBinding.btnGallery.setOnClickListener {
            mainViewModel.openGalleryEvent.call()
        }

        mBinding.btnBack.setOnClickListener {
            activity?.finish()
        }

        mBinding.btnSwitch.setOnClickListener {
            mBinding.cameraView.switchCamera()
            cameraViewModel.cameraBackForwardLiveData.value = mBinding.cameraView.isCameraBackForward
        }

        mBinding.cameraView.apply {
            setPictureSize(2048, 1536, true)
            setZOrderOnTop(false)
            setZOrderMediaOverlay(true)
            setOnCreateCallback {
                setFilterWithConfig(currentConfig.value)
            }
        }
    }

    private fun takePhoto(context: Context) {
        mBinding.cameraView.takePicture({ bitmap ->
            if (bitmap != null) {
                val filePath = ImageUtil.saveBitmap(bitmap, "${getInternalPath(context)}/${generateImageFileName()}")
                bitmap.recycle()

                if (!filePath.isNullOrEmpty()) {
                    val fileUri = Uri.fromFile(File(filePath)).also {
                        reScanFile(context, it)
                    }
                    mainViewModel.openPhotoPreviewEvent.value = Photo(fileUri, currentConfig, Source.CAMERA)
                }
            }
        }, null, NONE_CONFIG.value, 1.0f, true)
    }

    private fun updateModeView(newMode: CameraMode) {
        when (newMode) {
            CameraMode.Photo -> {
                mBinding.btnTakePhoto.isVisible = true
                mBinding.btnRecord.isVisible = false
            }
            CameraMode.Video -> {
                mBinding.btnTakePhoto.isInvisible = true
                mBinding.btnRecord.isVisible = true
            }
        }
    }

    private fun showFilters(visible: Boolean) {
        val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        if (visible && !mBinding.rcImgPreview.isVisible) {
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.alpha = 0.6F
                mBinding.rcImgPreview.isVisible = true
            }
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.animate()
                        .alpha(1F)
                        .setDuration(duration)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
            }
            mBinding.tvFilterName.post {
                mBinding.tvFilterName.alpha = 0.6F
                mBinding.tvFilterName.text = currentConfig.name
                mBinding.tvFilterName.isVisible = true
            }
            mBinding.tvFilterName.post {
                mBinding.tvFilterName.animate()
                        .alpha(1F)
                        .setDuration(duration)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
            }
            mBinding.btnPickFilters.isSelected = true
        } else if (!visible && mBinding.rcImgPreview.isVisible) {
            mBinding.rcImgPreview.animate()
                    .alpha(0F)
                    .setDuration(duration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.rcImgPreview.animate().setListener(null)
                            mBinding.rcImgPreview.isVisible = false
                        }
                    }).start()
            mBinding.tvFilterName.animate()
                    .alpha(0F)
                    .setDuration(duration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.tvFilterName.animate().setListener(null)
                            mBinding.tvFilterName.isVisible = false
                        }
                    })
                    .start()
            mBinding.btnPickFilters.isSelected = false
        }
    }

    private fun scheduleRecordTime() {
        mBinding.tvRecordingTime.isVisible = true
        val startTime = SystemClock.elapsedRealtime()
        timeRecordingFuture = scheduler.scheduleAtFixedRate({
            mainHandler.post {
                mBinding.tvRecordingTime.text = DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(SystemClock.elapsedRealtime() - startTime))
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    private fun cancelScheduledRecordTime() {
        mBinding.tvRecordingTime.isVisible = false
        timeRecordingFuture?.cancel(false)
    }

    override fun onResume() {
        super.onResume()
        mBinding.cameraView.onResume()
    }

    override fun onPause() {
        super.onPause()
        CameraInstance.getInstance().stopCamera()
        mBinding.cameraView.release {
            cameraViewModel.recordingStateLiveData.postValue(false)
        }
        mBinding.cameraView.onPause()
    }

    override fun onStop() {
        super.onStop()
        cancelScheduledRecordTime()
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduler.shutdown()
    }

    private fun onStartRecording() {
        cameraViewModel.recordingStateLiveData.value = true
        scheduleRecordTime()
    }

    private fun onFinishRecording(recordedFilePath: String) {
        cancelScheduledRecordTime()

        val fileUri = Uri.fromFile(File(recordedFilePath)).also {
            context?.run { reScanFile(this, it) }
        }
        mainViewModel.openVideoPreviewEvent.value = Video(fileUri, currentConfig, Source.CAMERA)
        cameraViewModel.recordingStateLiveData.postValue(false)
    }

    private inner class RecordListener : View.OnClickListener {

        private var isValid = true
        private var recordFilePath: String = ""

        override fun onClick(v: View?) {
            if (!isValid) {
                return
            }
            isValid = false

            if (!mBinding.cameraView.isRecording) {
                recordFilePath = "${getInternalPath(context!!)}/${generateVideoFileName()}"

                mBinding.cameraView.startRecording(recordFilePath, { success ->
                    if (success) {
                        mainHandler.post { onStartRecording() }
                        FileUtil.saveTextContent(recordFilePath, FileUtil.getPath() + "/lastVideoPath.txt")
                    } else {
                        mainHandler.post { Toast.makeText(this@CameraFragment.context, "Start recording failed", Toast.LENGTH_SHORT).show() }
                    }
                    isValid = true
                })
            } else {
                mBinding.cameraView.endRecording {
                    isValid = true
                    mainHandler.post { onFinishRecording(recordFilePath) }
                }
            }
        }
    }
}