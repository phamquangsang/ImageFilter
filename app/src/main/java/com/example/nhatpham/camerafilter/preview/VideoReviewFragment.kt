package com.example.nhatpham.camerafilter.preview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import com.example.nhatpham.camerafilter.databinding.FragmentVideoPreviewBinding
import org.wysaid.common.Common
import android.os.Handler
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.core.os.bundleOf
import androidx.work.OneTimeWorkRequest
import androidx.work.State
import androidx.work.WorkManager
import androidx.work.WorkStatus
import com.example.nhatpham.camerafilter.*
import com.example.nhatpham.camerafilter.jobs.GenerateFilteredVideoWorker
import com.example.nhatpham.camerafilter.models.Config
import com.example.nhatpham.camerafilter.models.Video
import com.example.nhatpham.camerafilter.models.isFromCamera
import com.example.nhatpham.camerafilter.models.isFromGallery
import com.example.nhatpham.camerafilter.utils.*
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


internal class VideoReviewFragment : ViewLifecycleFragment(), View.OnClickListener {

    private lateinit var mBinding: FragmentVideoPreviewBinding
    private val video: Video? by lazy {
        arguments?.getParcelable(EXTRA_VIDEO) as? Video
    }
    private val videoPathToSave by lazy {
        "${getPath()}/${generateVideoFileName()}"
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var videoPreviewViewModel: VideoPreviewViewModel
    private val mainHandler = Handler()
    private var scheduler = Executors.newSingleThreadScheduledExecutor()
    private var timeRecordingFuture: ScheduledFuture<*>? = null

    private val progressDialogFragment = ProgressDialogFragment()
    private lateinit var previewFiltersAdapter: PreviewFiltersAdapter
    private val currentConfig
        get() = videoPreviewViewModel.currentConfigLiveData.value ?: NONE_CONFIG

    private val animShortDuration by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    }
    private var lastIntervalUpdate = 0L
    private var isCompleted = true

    private lateinit var videoController: VideoController

    private val updateTimeIntervalTask = Runnable {
        if (videoController.isPlaying) {
            mBinding.tvRecordingTime.text = DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(
                    SystemClock.elapsedRealtime() - lastIntervalUpdate))
            mBinding.tvRecordingTime.isVisible = true
        }
    }

    private val playerListener = object : IPlayer.SimplePlayerCallback() {
        override fun onPlaybackStateUpdated(playbackState: PlaybackState) {
            mainHandler.post {
                when (playbackState.state) {
                    PlayerState.STATE_NONE, PlayerState.STATE_STOPPED, PlayerState.STATE_BUFFERING -> {
                        mBinding.tvRecordingTime.isVisible = false
                        mBinding.btnPlay.isVisible = true
                        cancelScheduledRecordTime()
                    }
                    PlayerState.STATE_PLAYING -> {
                        mBinding.btnPlay.isVisible = false
                        scheduleRecordTime()
                    }
                    PlayerState.STATE_PAUSED -> {
                        mBinding.btnPlay.isVisible = true
                        cancelScheduledRecordTime()
                    }
                }
            }
        }

        override fun onCompletion(uri: Uri) {
            mainHandler.post {
                isCompleted = true
                cancelScheduledRecordTime()
                mBinding.tvRecordingTime.isVisible = false
            }
        }

        override fun onError(message: String) {
            Log.d(Common.LOG_TAG, "Error occured! Stop playing, Err : $message")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_video_preview, container, false)
        initUI()
        return mBinding.root
    }

    private fun initUI() {
        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewFiltersAdapter = PreviewFiltersAdapter(context!!, EFFECT_CONFIGS, object : PreviewFiltersAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: Config) {
                videoPreviewViewModel.currentConfigLiveData.value = selectedConfig
            }
        })
        mBinding.rcImgPreview.adapter = previewFiltersAdapter
        val pos = previewFiltersAdapter.findConfigPos(video?.config ?: NONE_CONFIG)
        mBinding.rcImgPreview.scrollToPosition(pos ?: 0)

        mBinding.btnPickStickers.setOnClickListener(this)
        mBinding.btnPickFilters.setOnClickListener(this)
        mBinding.btnDone.setOnClickListener(this)
        mBinding.btnBack.setOnClickListener(this)
        mBinding.videoView.setOnClickListener(this)
        mBinding.videoView.apply {
            setZOrderOnTop(false)
            setFitFullView(true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainViewModel = getViewModel(activity!!)
        videoPreviewViewModel = getViewModel(this)

        videoPreviewViewModel.showFiltersEvent.observe(viewLifecycleOwner!!, Observer { active ->
            showFilters(active ?: false)
        })
        videoPreviewViewModel.currentConfigLiveData.value = video?.config ?: NONE_CONFIG
        videoPreviewViewModel.currentConfigLiveData.observe(viewLifecycleOwner!!, Observer { newConfig ->
            if (newConfig != null) {
                mBinding.videoView.setFilterWithConfig(newConfig.value)
                mBinding.tvFilterName.text = newConfig.name
                previewFiltersAdapter.setNewConfig(newConfig)
            }
        })

        videoController = VideoController(VideoPlayer(context!!), mBinding.videoView)
        videoController.addPlayerListener(playerListener)
        video?.uri?.let { videoController.play(it) }
        lifecycle.addObserver(videoController)
    }

    private fun scheduleRecordTime() {
        cancelScheduledRecordTime()
        timeRecordingFuture = scheduler.scheduleAtFixedRate({
            mainHandler.post(updateTimeIntervalTask)
        }, PROGRESS_UPDATE_INITIAL_INTERVAL, PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS)
    }

    private fun cancelScheduledRecordTime() {
        timeRecordingFuture?.cancel(false)
    }

    private fun showFilters(visible: Boolean) {
        if (visible) {
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
                mBinding.tvFilterName.text = videoPreviewViewModel.currentConfigLiveData.value?.name
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
        } else {
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

    override fun onResume() {
        super.onResume()
        videoController.addPlayerListener(playerListener)
    }

    override fun onDestroy() {
        scheduler.shutdown()
        video?.let {
            if (it.isFromCamera())
                checkToDeleteTempFile(it.uri)
        }
        super.onDestroy()
    }

    private fun checkToDeleteTempFile(uri: Uri) {
        if (isFileUri(uri)) {
            File(uri.path).apply {
                if (exists()) {
                    delete()
                    activity?.let {
                        reScanFile(it, uri)
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            mBinding.btnPickFilters -> toggleFilters()
            mBinding.btnPickStickers -> toggleStickers()
            mBinding.videoView -> checkToPlay()
            mBinding.btnBack -> exit()
            mBinding.btnDone -> saveVideo()
        }
    }

    private fun toggleFilters() {
        videoPreviewViewModel.showFiltersEvent.value = videoPreviewViewModel.showFiltersEvent.value?.not() ?: true
    }

    private fun toggleStickers() {
        mBinding.btnPickStickers.isSelected = !mBinding.btnPickStickers.isSelected
    }

    private fun checkToPlay() {
        mBinding.videoView.isVisible = true

        val uri = video?.uri
        if (uri != null) {
            if (isCompleted) {
                isCompleted = false
                mBinding.videoView.setFilterWithConfig(currentConfig.value)
                lastIntervalUpdate = SystemClock.elapsedRealtime() + PROGRESS_UPDATE_INITIAL_INTERVAL

                videoController.play(uri)
            } else {
                when (videoController.currentState.state) {
                    PlayerState.STATE_PAUSED -> {
                        lastIntervalUpdate = SystemClock.elapsedRealtime() - videoController.currentPosition + PROGRESS_UPDATE_INITIAL_INTERVAL
                        videoController.play(uri)
                    }
                    PlayerState.STATE_PLAYING -> {
                        videoController.pause()
                    }
                }
            }
        }
    }

    private fun exit() {
        activity?.run {
            if(supportFragmentManager.backStackEntryCount == 0)
                finish()
            else supportFragmentManager.popBackStack()
        }
    }

    private fun saveVideo() {
        val currentVideo = video
        if (currentVideo != null && (isMediaStoreVideoUri(currentVideo.uri) ||
                        (isFileUri(currentVideo.uri) && File(currentVideo.uri.path).exists()))) {
            videoController.stop()

            if (currentVideo.isFromGallery() && currentConfig == NONE_CONFIG) {
                when {
                    isMediaStoreImageUri(currentVideo.uri) -> {
                        val path = getPathFromMediaUri(context!!, currentVideo.uri)
                        if(!path.isNullOrEmpty()) {
                            mainViewModel.doneEditEvent.value = Uri.fromFile(File(path))
                        } else {
                            mainViewModel.doneEditEvent.value = null
                        }
                    }
                    isFileUri(currentVideo.uri) && File(currentVideo.uri.path).exists() -> {
                        mainViewModel.doneEditEvent.value = currentVideo.uri
                    }
                    else -> mainViewModel.doneEditEvent.value = null
                }
            } else {
                val inputPath = if (currentVideo.isFromGallery())
                    getPathFromMediaUri(context!!, currentVideo.uri)
                else
                    currentVideo.uri.toString()

                if (inputPath == null) {
                    mainViewModel.doneEditEvent.value = null
                    return
                }

                scheduleGenerateFilteredVideoNow(inputPath, currentConfig.value, videoPathToSave)?.observe(viewLifecycleOwner!!,
                        Observer { workStatus ->
                            if (workStatus != null) {
                                if (workStatus.state.isFinished) {
                                    progressDialogFragment.dismiss()

                                    if (workStatus.state == State.SUCCEEDED) {
                                        val outputUri = workStatus.outputData.getString(GenerateFilteredVideoWorker.KEY_RESULT, "")
                                        if (outputUri != null && !outputUri.isEmpty()) {
                                            mainViewModel.doneEditEvent.value = Uri.parse(outputUri)
                                        } else {
                                            mainViewModel.doneEditEvent.value = null
                                        }
                                    } else {
                                        mainViewModel.doneEditEvent.value = null
                                    }
                                } else {
                                    when (workStatus.state) {
                                        State.ENQUEUED -> {
                                            progressDialogFragment.show(fragmentManager, ProgressDialogFragment::class.java.simpleName)
                                        }
                                    }
                                }
                            }
                        }
                )
            }
        } else mainViewModel.doneEditEvent.value = null
    }

    private fun scheduleGenerateFilteredVideoNow(inputPath: String, config: String, outputPath: String): LiveData<WorkStatus>? {
        val workManager = WorkManager.getInstance()
        if (workManager != null) {
            val generateFilteredVideoWorkRequest = OneTimeWorkRequest.Builder(GenerateFilteredVideoWorker::class.java)
                    .setInputData(GenerateFilteredVideoWorker.data(inputPath, config, outputPath))
                    .build()
            workManager.enqueue(generateFilteredVideoWorkRequest)
            return workManager.getStatusById(generateFilteredVideoWorkRequest.id)
        }
        return null
    }

    companion object {
        private const val EXTRA_VIDEO = "video"
        private const val PROGRESS_UPDATE_INTERNAL: Long = 1000
        private const val PROGRESS_UPDATE_INITIAL_INTERVAL: Long = 100

        fun newInstance(video: Video): VideoReviewFragment {
            return VideoReviewFragment().apply {
                arguments = bundleOf(EXTRA_VIDEO to video)
            }
        }
    }
}