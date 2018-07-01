package com.example.nhatpham.camerafilter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
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
import android.util.TimeUtils
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
    private lateinit var viewModel: PreviewViewModel
    private lateinit var previewImagesAdapter: PreviewImagesAdapter
    private lateinit var modesAdapter: ModesAdapter

    private var currentConfig: String? = null
    private val mainHandler = Handler()
    private var isRecording = false
    private var currentMode = "Photo"
    private var snapHelper = PagerSnapHelper()
    private var scheduler = Executors.newSingleThreadScheduledExecutor()
    private var timeRecordingFuture : ScheduledFuture<*>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        viewModel = ViewModelProviders.of(activity!!).get(PreviewViewModel::class.java)
        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewImagesAdapter = PreviewImagesAdapter(context!!, MainActivity.EFFECT_CONFIGS.asList(), object : PreviewImagesAdapter.OnItemInteractListener {

            override fun onConfigSelected(selectedConfig: String) {
                currentConfig = selectedConfig
                mBinding.cameraView.setFilterWithConfig(selectedConfig)
            }
        })
        previewImagesAdapter.imageUri = ""
        mBinding.rcImgPreview.adapter = previewImagesAdapter

        mBinding.rcModes.layoutManager = CustomLayoutManager(context!!, LinearLayoutManager.HORIZONTAL, false)
        snapHelper.attachToRecyclerView(mBinding.rcModes)
        modesAdapter = ModesAdapter(arrayListOf("Photo", "Video"), object : ModesAdapter.OnItemInteractListener {
            override fun onModeSelected(mode: String, position: Int) {
                if(isRecording)
                    return
                currentMode = mode
                updateModeDisplay()
                mBinding.rcModes.smoothScrollToPosition(position)
            }
        })
        mBinding.rcModes.adapter = modesAdapter
        mBinding.rcModes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if(newState != RecyclerView.SCROLL_STATE_IDLE && isRecording)
                    return

                val layoutManager = recyclerView?.layoutManager
                if(layoutManager is LinearLayoutManager) {
                    val pos = layoutManager.getPosition(snapHelper.findSnapView(layoutManager))
                    if(pos != RecyclerView.NO_POSITION) {
                        currentMode = modesAdapter.getItem(pos)
                        updateModeDisplay()
                    }
                }
            }
        })

        mBinding.btnTakePhoto.setOnClickListener {
            if(!isRecording && currentMode == "Photo") {
                mBinding.cameraView.takePicture({ bitmap ->
                    if (bitmap != null) {
                        val filePath = ImageUtil.saveBitmap(bitmap)
                        bitmap.recycle()
                        activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(filePath))))
                        viewModel.openPreviewEvent.value = filePath
                    }
                }, null, currentConfig, 1.0f, true)
            }
        }
        mBinding.btnRecord.setOnClickListener(RecordListener())

        mBinding.btnPickFilters.setOnClickListener {
            if (!mBinding.rcImgPreview.isVisible) {
                mBinding.rcImgPreview.animate()
                        .alpha(1F)
                        .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator?) {
                                mBinding.rcImgPreview.animate().setListener(null)
                                mBinding.rcImgPreview.alpha = 0.6F
                                mBinding.rcImgPreview.isVisible = true
                            }
                        })
                        .start()
                mBinding.btnPickFilters.isSelected = true
            } else {
                mBinding.rcImgPreview.animate()
                        .alpha(0F)
                        .setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                mBinding.rcImgPreview.animate().setListener(null)
                                mBinding.rcImgPreview.isVisible = false
                            }
                        }).start()
                mBinding.btnPickFilters.isSelected = false
            }
        }
        mBinding.btnGallery.setOnClickListener {

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

    private fun updateModeDisplay() {
        when (currentMode) {
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

    private fun updateRecordingDisplay() {
        mBinding.btnRecord.setImageResource(if(isRecording) R.drawable.stop_recording else R.drawable.start_record)
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
        mBinding.cameraView.release(null)
        mBinding.cameraView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mBinding.cameraView.onResume()
    }

    override fun onStop() {
        super.onStop()
        cancelScheduleRecordTime()
        scheduler.shutdown()
    }

    inner class RecordListener : View.OnClickListener {

        private var isValid = true
        private var recordFilename: String = ""

        override fun onClick(v: View?)  {
            val LOG_TAG = CameraRecordGLSurfaceView.LOG_TAG;

            if (!isValid) {
                Log.e(LOG_TAG, "Please wait for the call...")
                return
            }
            isValid = false

            if (!mBinding.cameraView.isRecording) {
                Log.i(LOG_TAG, "Start recording...")
                recordFilename = ImageUtil.getPath() + "/rec_" + System.currentTimeMillis() + ".mp4"
                //                recordFilename = ImageUtil.getPath(CameraDemoActivity.this, false) + "/rec_1.mp4";
                mBinding.cameraView.startRecording(recordFilename, { success ->
                    if (success) {
                        mainHandler.post {
                            isRecording = true
                            updateRecordingDisplay()
                            scheduleRecordTime()
                        }
                        FileUtil.saveTextContent(recordFilename, FileUtil.getPath() + "/lastVideoPath.txt")
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

                    mainHandler.post {
                        isRecording = false
                        updateRecordingDisplay()
                        cancelScheduleRecordTime()
                    }
                }
            }
        }
    }
}