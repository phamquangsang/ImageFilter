package com.example.nhatpham.camerafilter.preview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import com.example.nhatpham.camerafilter.databinding.FragmentVideoPreviewBinding
import org.wysaid.common.Common
import org.wysaid.view.VideoPlayerGLSurfaceView
import android.os.Handler
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.core.graphics.applyCanvas
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.example.nhatpham.camerafilter.*
import com.example.nhatpham.camerafilter.models.Config
import com.example.nhatpham.camerafilter.models.Video
import com.example.nhatpham.camerafilter.models.isFromCamera
import com.example.nhatpham.camerafilter.utils.*
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.view.ImageGLSurfaceView
import java.io.File
import java.nio.file.Files.delete
import java.nio.file.Files.exists
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


internal class VideoPreviewFragment : Fragment() {

    private lateinit var mBinding: FragmentVideoPreviewBinding
    private val video: Video? by lazy {
        arguments?.getParcelable(EXTRA_VIDEO) as? Video
    }
    private val videoPathToSave by lazy {
        "${getPath()}/${generateVideoFileName()}"
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var videoPreviewViewModel: PhotoPreviewViewModel
    private val mainHandler = Handler()
    private var mediaPlayer: MediaPlayer? = null
    private var scheduler = Executors.newScheduledThreadPool(2)
    private var timeRecordingFuture: ScheduledFuture<*>? = null

    private val progressDialogFragment = ProgressDialogFragment()
    private lateinit var previewFiltersAdapter: PreviewFiltersAdapter
    private var canPlay = true
    private var currentBitmap: Bitmap? = null
    private val currentConfig
        get() = videoPreviewViewModel.currentConfigLiveData.value ?: NONE_CONFIG

    private val playCompletionCallback = object : VideoPlayerGLSurfaceView.PlayCompletionCallback {
        override fun playComplete(player: MediaPlayer) {
            cancelScheduleRecordTime()
            mBinding.videoView.isVisible = false
            showThumbnail(true)
            mBinding.btnPlay.isVisible = true
            mediaPlayer = player
        }

        override fun playFailed(player: MediaPlayer, what: Int, extra: Int): Boolean {
            Log.d(Common.LOG_TAG, String.format("Error occured! Stop playing, Err code: %d, %d", what, extra))
            canPlay = false
            return true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_video_preview, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        videoPreviewViewModel = ViewModelProviders.of(this).get(PhotoPreviewViewModel::class.java)

        videoPreviewViewModel.showFiltersEvent.observe(viewLifecycleOwner, Observer { active ->
            showFilters(active ?: false)
        })

        videoPreviewViewModel.currentConfigLiveData.value = video?.config ?: NONE_CONFIG
        videoPreviewViewModel.currentConfigLiveData.observe(viewLifecycleOwner, Observer { newConfig ->
            if (newConfig != null) {
                if (mediaPlayer == null || !mediaPlayer!!.isPlaying) {
                    showThumbnail(true)
                }
                mBinding.videoView.setFilterWithConfig(newConfig.value)
                mBinding.tvFilterName.text = newConfig.name
                previewFiltersAdapter.setNewConfig(newConfig)
            }
        })

        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewFiltersAdapter = PreviewFiltersAdapter(context!!, EFFECT_CONFIGS, object : PreviewFiltersAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: Config) {
                videoPreviewViewModel.currentConfigLiveData.value = selectedConfig
            }
        })
        mBinding.rcImgPreview.adapter = previewFiltersAdapter
        val pos = previewFiltersAdapter.findConfigPos(video?.config ?: NONE_CONFIG)
        mBinding.rcImgPreview.scrollToPosition(pos ?: 0)

        mBinding.videoView.apply {
            setZOrderOnTop(false)
            setPlayerInitializeCallback { player ->
                player.setOnBufferingUpdateListener { _, percent ->
                    if (percent == 100) {
                        player.setOnBufferingUpdateListener(null)
                    }
                }
            }
            isVisible = false
        }

        mBinding.imgVideoThumb.apply {
            displayMode = ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FILL
            setSurfaceCreatedCallback {
                setImageBitmap(currentBitmap)
                setFilterWithConfig(currentConfig.value)
            }
        }

        mBinding.btnPlay.setOnClickListener {
            if (mediaPlayer != null) {
                mBinding.videoView.isVisible = true
                startPlayingVideo()
            } else {
                mBinding.videoView.isVisible = true
                mBinding.videoView.setVideoUri(video?.uri, { player ->
                    mediaPlayer = player
                    startPlayingVideo()
                }, playCompletionCallback)
            }
            mBinding.btnPlay.isVisible = false
        }

        mBinding.btnPickStickers.setOnClickListener {
            mBinding.btnPickStickers.isSelected = !mBinding.btnPickStickers.isSelected
        }

        mBinding.btnPickFilters.setOnClickListener {
            videoPreviewViewModel.showFiltersEvent.value = videoPreviewViewModel.showFiltersEvent.value?.not() ?: true
        }

        mBinding.btnDone.setOnClickListener {
            val currentVideo = video
            if (currentVideo != null && (isMediaStoreVideoUri(currentVideo.uri) || isFileUri(currentVideo.uri))) {
                mediaPlayer?.run {
                    if (isPlaying)
                        stop()
                }
                if (!File(currentVideo.uri.path).exists()) {
                    mainViewModel.doneEditEvent.value = null
                    return@setOnClickListener
                }
                progressDialogFragment.show(fragmentManager, ProgressDialogFragment::class.java.simpleName)

                if(isExternalStorageWritable()) {
                    scheduler.submit {
                        val result = generateFilteredVideo(videoPathToSave, currentConfig.value)
                        mainHandler.post {
                            progressDialogFragment.dismiss()
                        }
                        mainViewModel.doneEditEvent.postValue(result)
                    }
                }
            } else mainViewModel.doneEditEvent.value = null
        }

        mBinding.btnBack.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
        showThumbnail(true)
    }

    private fun startPlayingVideo() {
        mediaPlayer?.run {
            showThumbnail(false)
            mBinding.videoView.setFilterWithConfig(currentConfig.value)
            start()
            scheduleRecordTime()
        }
    }

    private fun showThumbnail(visible: Boolean, config: Config = currentConfig) {
        if (visible) {
            mBinding.imgVideoThumb.isVisible = true

            if (currentBitmap == null) {
                Glide.with(this)
                        .asBitmap()
                        .load(video?.uri)
                        .apply(RequestOptions.skipMemoryCacheOf(true))
                        .apply(RequestOptions.bitmapTransform(object : BitmapTransformation() {
                            override fun updateDiskCacheKey(messageDigest: MessageDigest) {
                                val videoUri = video?.uri
                                if (videoUri != null) {
                                    messageDigest.update("$videoUri-${config.name}".toByteArray())
                                }
                            }

                            override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
                                return Bitmap.createBitmap(toTransform.width, toTransform.height, Bitmap.Config.ARGB_8888).applyCanvas {
                                    drawBitmap(toTransform, 0F, 0F, null)
                                }
                            }

                        })).listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                                return false
                            }

                            override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                currentBitmap = resource
                                mBinding.imgVideoThumb.post {
                                    mBinding.imgVideoThumb.setFilterWithConfig(config.value)
                                }
                                mBinding.imgVideoThumb.setImageBitmap(currentBitmap)
                                return false
                            }
                        })
                        .submit()
            } else {
                mBinding.imgVideoThumb.setFilterWithConfig(config.value)
            }
        } else {
            mBinding.imgVideoThumb.isVisible = false
        }
    }

    private fun generateFilteredVideo(outputPath: String, config: String): Uri? {
        val result = CGEFFmpegNativeLibrary.generateVideoWithFilter(outputPath, video?.uri.toString(), config, 1.0f, null, CGENativeLibrary.TextureBlendMode.CGE_BLEND_OVERLAY, 1.0f, false)
        return if (result) {
            Uri.fromFile(File(outputPath)).also {
                reScanFile(it)
            }
        } else null
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
                mBinding.tvFilterName.text = videoPreviewViewModel.currentConfigLiveData.value?.name
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

    private fun reScanFile(videoUri: Uri) {
        activity?.let {
            reScanFile(it, videoUri)
        }
    }

    override fun onResume() {
        super.onResume()
        mBinding.imgVideoThumb.onResume()
        mBinding.videoView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mBinding.imgVideoThumb.release()
        mBinding.imgVideoThumb.onPause()
        mBinding.videoView.release()
        mBinding.videoView.onPause()
    }

    override fun onStop() {
        super.onStop()
        cancelScheduleRecordTime()
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

    companion object {
        private const val EXTRA_VIDEO = "EXTRA_VIDEO"

        fun newInstance(video: Video): VideoPreviewFragment {
            return VideoPreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_VIDEO, video)
                }
            }
        }
    }
}