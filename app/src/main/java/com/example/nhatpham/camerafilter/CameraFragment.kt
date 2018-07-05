package com.example.nhatpham.camerafilter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.example.nhatpham.camerafilter.databinding.FragmentCameraBinding
import org.wysaid.camera.CameraInstance
import org.wysaid.myUtils.FileUtil
import org.wysaid.myUtils.ImageUtil
import org.wysaid.view.CameraRecordGLSurfaceView
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment() {

    private lateinit var mBinding: FragmentCameraBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var previewImagesAdapter: PreviewImagesAdapter
    private lateinit var modesAdapter: ModesAdapter

    private val mainHandler = Handler()
    private var snapHelper = PagerSnapHelper()
    private var scheduler = Executors.newSingleThreadScheduledExecutor()
    private var timeRecordingFuture: ScheduledFuture<*>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        cameraViewModel = ViewModelProviders.of(this).get(CameraViewModel::class.java)
        mBinding.cameraviewmodel = cameraViewModel

        cameraViewModel.showFiltersEvent.observe(viewLifecycleOwner, Observer { active ->
            showFilters(active ?: false)
        })

        cameraViewModel.currentModeLiveData.observe(viewLifecycleOwner, Observer {
            updateModeView(it ?: "Photo")
        })

        cameraViewModel.currentConfigLiveData.observe(viewLifecycleOwner, Observer { newConfig ->
            if(newConfig != null) {
                mBinding.cameraView.setFilterWithConfig(newConfig.value)
                mBinding.tvFilterName.text = newConfig.name
                previewImagesAdapter.setNewConfig(newConfig)
            }
        })

        cameraViewModel.recordingStateLiveData.observe(viewLifecycleOwner, Observer {
            val active = it == true
            mBinding.btnRecord.setImageResource(if (active) R.drawable.stop_recording else R.drawable.start_record)
            showFilters(active.not())
            cameraViewModel.isRecording.set(active)
        })

        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewImagesAdapter = PreviewImagesAdapter(context!!, EFFECT_CONFIGS, object : PreviewImagesAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: Config) {
                cameraViewModel.currentConfigLiveData.value = selectedConfig
            }
        })
        previewImagesAdapter.imageUri = ""
        mBinding.rcImgPreview.adapter = previewImagesAdapter

        mBinding.rcModes.layoutManager = CustomLayoutManager(context!!, LinearLayoutManager.HORIZONTAL, false)
        snapHelper.attachToRecyclerView(mBinding.rcModes)
        modesAdapter = ModesAdapter(arrayListOf("Photo", "Video"), object : ModesAdapter.OnItemInteractListener {
            override fun onModeSelected(mode: String, position: Int) {
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

        mBinding.btnTakePhoto.setOnClickListener {
            mBinding.cameraView.takePicture({ bitmap ->
                if (bitmap != null) {
                    val filePath = ImageUtil.saveBitmap(bitmap, "${getPath()}/${generateImageFileName()}")
                    bitmap.recycle()

                    val fileUri = Uri.fromFile(File(filePath))
                    reScanFile(fileUri)
                    mainViewModel.openPhotoPreviewEvent.value = fileUri
                }
            }, null, cameraViewModel.currentConfigLiveData.value?.value ?: DEFAULT_CONFIG.value, 1.0f, true)
        }
        mBinding.btnRecord.setOnClickListener(RecordListener())

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
        }

        mBinding.cameraView.apply {
            setPictureSize(2048, 2048, true) // > 4MP
            setZOrderOnTop(false)
            setZOrderMediaOverlay(true)
        }
    }

    private fun updateModeView(newMode: String) {
        when (newMode) {
            "Photo" -> {
                mBinding.btnTakePhoto.isVisible = true
                mBinding.btnRecord.isVisible = false
            }
            "Video" -> {
                mBinding.btnTakePhoto.isInvisible = true
                mBinding.btnRecord.isVisible = true
            }
        }
    }

    private fun showFilters(visible: Boolean) {
        val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        if (visible) {
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
                mBinding.tvFilterName.text = cameraViewModel.currentConfigLiveData.value?.name
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
        } else {
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

    private fun cancelScheduleRecordTime() {
        mBinding.tvRecordingTime.isVisible = false
        timeRecordingFuture?.cancel(false)
    }

    override fun onPause() {
        super.onPause()
        CameraInstance.getInstance().stopCamera()
        mBinding.cameraView.release {
            cameraViewModel.recordingStateLiveData.postValue(false)
        }
        mBinding.cameraView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mBinding.cameraView.onResume()
    }

    override fun onStop() {
        super.onStop()
        cancelScheduleRecordTime()
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
        val fileUri = Uri.fromFile(File(recordedFilePath))
        reScanFile(fileUri)
        cancelScheduleRecordTime()
        cameraViewModel.recordingStateLiveData.value = false
        mainViewModel.openVideoPreviewEvent.value = fileUri
    }

    private fun reScanFile(fileUri: Uri) {
        context?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri))
    }

    inner class RecordListener : View.OnClickListener {

        private var isValid = true
        private var recordFilePath: String = ""

        override fun onClick(v: View?) {
            val LOG_TAG = CameraRecordGLSurfaceView.LOG_TAG;

            if (!isValid) {
                Log.e(LOG_TAG, "Please wait for the call...")
                return
            }
            isValid = false

            if (!mBinding.cameraView.isRecording) {
                Log.i(LOG_TAG, "Start recording...")
                recordFilePath = "${getPath()}/${generateVideoFileName()}"
                //                recordFilename = ImageUtil.getPath(CameraDemoActivity.this, false) + "/rec_1.mp4";
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
                Log.i(LOG_TAG, "End recording...")
                mBinding.cameraView.endRecording {
                    Log.i(LOG_TAG, "End recording OK")
                    isValid = true

                    mainHandler.post { onFinishRecording(recordFilePath) }
                }
            }
        }
    }
}