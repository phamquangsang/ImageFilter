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
import com.example.nhatpham.camerafilter.custom.ScaledCenterLayoutManager
import com.example.nhatpham.camerafilter.custom.ViewLifecycleFragment
import com.example.nhatpham.camerafilter.databinding.FragmentCameraFiltersBinding
import com.example.nhatpham.camerafilter.models.*
import com.example.nhatpham.camerafilter.utils.*
import org.wysaid.myUtils.FileUtil
import org.wysaid.myUtils.ImageUtil
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

internal class CameraFragment : ViewLifecycleFragment(), View.OnClickListener {

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
    private val animShortDuration by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    }
    private val currentMode : CameraMode
    get() = cameraViewModel.currentModeLiveData.value ?: CameraMode.Photo

    private val recordListener = RecordListener()
    private var lastModeSelectedPos = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera_filters, container, false)
        initUI()
        return mBinding.root
    }

    private fun initUI() {
        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
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
                lastModeSelectedPos = position
                cameraViewModel.currentModeLiveData.value = mode
                mBinding.rcModes.smoothScrollToPosition(position)
            }
        })
        mBinding.rcModes.adapter = modesAdapter
        mBinding.rcModes.scrollToPosition(lastModeSelectedPos)
        mBinding.rcModes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var scrolled = false

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE && scrolled) {
                    scrolled = false
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
            }

            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dx != 0 || dy != 0) {
                    scrolled = true
                }
            }
        })

        mBinding.imgGallery.clickWithDebounce(listener = this)
        mBinding.fabAction.clickWithDebounce(listener = this)
        mBinding.btnPickFilters.clickWithDebounce(300, listener = this)
        mBinding.btnBack.clickWithDebounce(listener = this)
        mBinding.btnSwitch.clickWithDebounce(listener = this)

        lifecycle.addObserver(mBinding.cameraView)
        mBinding.cameraView.setOnCreateCallback {
            mBinding.cameraView.setFilterWithConfig(currentConfig.value)
        }
        mBinding.cameraView.setReleaseOKCallback {
            cameraViewModel.recordingStateLiveData.postValue(false)
        }
        mBinding.root.afterMeasured {
            mBinding.cameraView.setCameraReadyCallback {
                mBinding.cameraView.setPictureSize(width, height, true)
                mBinding.cameraView.setZOrderOnTop(false)
                mBinding.cameraView.setZOrderMediaOverlay(true)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainViewModel = getViewModel(activity!!)
        cameraViewModel = getViewModel(this)
        mBinding.cameraviewmodel = cameraViewModel

        cameraViewModel.showFiltersEvent.observe(viewLifecycleOwner!!, Observer { active ->
            showFilters(active ?: false)
        })

        cameraViewModel.currentModeLiveData.observe(viewLifecycleOwner!!, Observer {
            updateModeView(it ?: CameraMode.Photo)
        })

        if (cameraViewModel.cameraBackForwardLiveData.value == null) {
            cameraViewModel.cameraBackForwardLiveData.value = mBinding.cameraView.isCameraBackForward
        }
        cameraViewModel.cameraBackForwardLiveData.observe(viewLifecycleOwner!!, Observer {
            if (mBinding.cameraView.isCameraBackForward != it) {
                mBinding.cameraView.switchCamera()
            }
        })

        cameraViewModel.currentConfigLiveData.observe(viewLifecycleOwner!!, Observer { newConfig ->
            if (newConfig != null) {
                mBinding.cameraView.setFilterWithConfig(newConfig.value)
                mBinding.tvFilterName.text = newConfig.name
                previewFiltersAdapter.setNewConfig(newConfig)
            }
        })

        cameraViewModel.recordingStateLiveData.observe(viewLifecycleOwner!!, Observer {
            if(currentMode == CameraMode.Video) {
                val active = it == true
                updateRecordStateView(active)
                cameraViewModel.isRecording.set(active)

                if (active) showFilters(false)
                else cameraViewModel.showFiltersEvent.value = cameraViewModel.showFiltersEvent.value
            }
        })

        when (PREVIEW_TYPE) {
            PreviewType.Photo -> {
                mBinding.rcModes.isInvisible = true
                cameraViewModel.currentModeLiveData.value = CameraMode.Photo
            }
            PreviewType.Video -> {
                mBinding.rcModes.isInvisible = true
                cameraViewModel.currentModeLiveData.value = CameraMode.Video
            }
            else -> {
                mBinding.rcModes.isVisible = true
            }
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

    private fun updateModeView(newMode: CameraMode) {
        when (newMode) {
            CameraMode.Photo -> {
                mBinding.imgCamera.isVisible = true
                mBinding.imgStartRecording.isVisible = false
            }
            CameraMode.Video -> {
                mBinding.imgCamera.isVisible = false
                mBinding.imgStartRecording.isVisible = true
            }
        }
    }

    private fun updateRecordStateView(active: Boolean) {
        if(active) {
            mBinding.imgStartRecording.animate()
                    .scaleX(0.35f)
                    .scaleY(0.35f)
                    .setDuration(animShortDuration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator?) {
                            mBinding.imgStartRecording.animate().setListener(null)
                            mBinding.imgStopRecording.post {
                                mBinding.imgStopRecording.scaleX = 0.35f
                                mBinding.imgStopRecording.scaleY = 0.35f
                                mBinding.imgStopRecording.isVisible = true
                            }
                            mBinding.imgStopRecording.post {
                                mBinding.imgStopRecording.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(animShortDuration)
                                        .setInterpolator(AccelerateDecelerateInterpolator())
                                        .start()
                            }
                        }
                    })
                    .start()
        } else {
            mBinding.imgStartRecording.isVisible = true
            mBinding.imgStopRecording.isVisible = false
        }
    }

    private fun showFilters(visible: Boolean) {
        if (visible && !mBinding.rcImgPreview.isVisible) {
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.alpha = 0.6F
                mBinding.rcImgPreview.isVisible = true
            }
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.animate()
                        .alpha(1F)
                        .setDuration(animShortDuration)
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
                        .setDuration(animShortDuration)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
            }
            mBinding.btnPickFilters.isSelected = true
        } else if (!visible && mBinding.rcImgPreview.isVisible) {
            mBinding.rcImgPreview.animate()
                    .alpha(0F)
                    .setDuration(animShortDuration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.rcImgPreview.animate().setListener(null)
                            mBinding.rcImgPreview.isVisible = false
                        }
                    }).start()
            mBinding.tvFilterName.animate()
                    .alpha(0F)
                    .setDuration(animShortDuration)
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

    override fun onStop() {
        super.onStop()
        cancelScheduledRecordTime()
    }

    override fun onDestroy() {
        scheduler.shutdown()
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        when (v) {
            mBinding.btnPickFilters -> toggleFilters()
            mBinding.fabAction -> {
                if(currentMode == CameraMode.Photo) context?.let { takePhoto(it) }
                else if(currentMode == CameraMode.Video) recordListener.onClick(v)
            }
            mBinding.imgGallery -> openGallery()
            mBinding.btnSwitch -> switchCamera()
            mBinding.btnBack -> exit()
        }
    }

    private fun toggleFilters() {
        cameraViewModel.showFiltersEvent.value = cameraViewModel.showFiltersEvent.value?.not() ?: true
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

    private fun switchCamera() {
        mBinding.cameraView.switchCamera()
        cameraViewModel.cameraBackForwardLiveData.value = mBinding.cameraView.isCameraBackForward
    }

    private fun openGallery() {
        mainViewModel.openGalleryEvent.call()
    }

    private fun exit() {
        activity?.finish()
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