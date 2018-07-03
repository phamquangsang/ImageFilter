package com.example.nhatpham.camerafilter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.databinding.DataBindingUtil
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
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.os.Handler
import android.os.SystemClock
import android.provider.MediaStore
import android.text.format.DateUtils
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGEFFmpegNativeLibrary.generateVideoWithFilter
import org.wysaid.nativePort.CGENativeLibrary
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class VideoPreviewFragment : Fragment() {

    private lateinit var mBinding: FragmentVideoPreviewBinding
    private val videoUri: Uri? by lazy {
        arguments?.getParcelable(EXTRA_VIDEO_URI) as? Uri
    }

    private val mainHandler = Handler()
    private var mediaPlayer: MediaPlayer? = null
    private var scheduler = Executors.newSingleThreadScheduledExecutor()
    private var timeRecordingFuture : ScheduledFuture<*>? = null

    private lateinit var previewImagesAdapter: PreviewImagesAdapter
    private var currentConfig: String? = null

    private val playCompletionCallback = object : VideoPlayerGLSurfaceView.PlayCompletionCallback {
        override fun playComplete(player: MediaPlayer) {
            cancelScheduleRecordTime()
            mBinding.imgVideoThumb.isVisible = true
            mBinding.btnPlay.isVisible = true
            mediaPlayer = player
        }

        override fun playFailed(player: MediaPlayer, what: Int, extra: Int): Boolean {
            Log.d(Common.LOG_TAG, String.format("Error occured! Stop playing, Err code: %d, %d", what, extra))
            return true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_video_preview, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewImagesAdapter = PreviewImagesAdapter(context!!, EFFECT_CONFIGS.keys.toList(), object : PreviewImagesAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: String) {
                currentConfig = selectedConfig
                mBinding.videoView.setFilterWithConfig(selectedConfig)
            }
        })
        mBinding.rcImgPreview.adapter = previewImagesAdapter

        mBinding.videoView.setZOrderOnTop(false)
        mBinding.videoView.setZOrderMediaOverlay(true)
        mBinding.videoView.setPlayerInitializeCallback({ player ->
            //针对网络视频进行进度检查
            player.setOnBufferingUpdateListener { _, percent ->
                if (percent == 100) {
                    player.setOnBufferingUpdateListener(null)
                }
            }
        })

        mBinding.videoView.setVideoUri(videoUri, { player ->
            mediaPlayer = player
        }, playCompletionCallback)

        mBinding.imgVideoThumb.setImageBitmap(getThumbnail(context!!, videoUri!!))

        mBinding.btnPlay.setOnClickListener {
            mediaPlayer?.run {
                start()
                mBinding.imgVideoThumb.isVisible = false
                mBinding.btnPlay.isVisible = false
                scheduleRecordTime()
            }
        }

        mBinding.btnPickStickers.setOnClickListener {
            mBinding.btnPickStickers.isSelected = !mBinding.btnPickStickers.isSelected
        }

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

        mBinding.btnDone.setOnClickListener {
            val outputFileName = "${getPath()}/${generateVideoFileName()}"
            CGEFFmpegNativeLibrary.generateVideoWithFilter(outputFileName, videoUri.toString(), currentConfig, 1.0f, null, CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV, 1.0f, false)
            activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(outputFileName))))
            activity?.supportFragmentManager?.popBackStack()
        }

        mBinding.btnBack.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
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

    override fun onResume() {
        super.onResume()
        mBinding.videoView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mBinding.videoView.release()
        mBinding.videoView.onPause()
    }

    override fun onStop() {
        super.onStop()
        cancelScheduleRecordTime()
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduler.shutdown()
    }

    companion object {
        private const val EXTRA_VIDEO_URI = "EXTRA_VIDEO_URI"

        fun newInstance(videoUri: Uri): VideoPreviewFragment {
            return VideoPreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_VIDEO_URI, videoUri)
                }
            }
        }
    }
}