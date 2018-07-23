package com.example.nhatpham.camerafilter.preview

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.net.Uri
import android.view.Surface
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.video.VideoListener
import org.wysaid.view.VideoPlayerGLSurfaceView

internal class VideoController(private val videoPlayer: VideoPlayer,
                               private val videoPlayerGLSurfaceView: VideoPlayerGLSurfaceView) : LifecycleObserver {

    private val videoListener = object : VideoListener {

        override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
            videoPlayerGLSurfaceView.createFrameRenderer(width, height)
        }

        override fun onRenderedFirstFrame() {
            videoPlayerGLSurfaceView.setHandleCurrentFrame(true)
            videoPlayer.pause()
        }
    }

    private var currentUri: Uri? = null
    private val surfaceTextureCreated
    get() = videoPlayerGLSurfaceView.surfaceTexture != null

    val isPlaying: Boolean
    get() = videoPlayer.isPlaying

    val currentState: PlaybackState
    get() = videoPlayer.currentState

    val currentPosition: Long
    get() = videoPlayer.exoPlayer?.currentPosition ?: 0L

    private val playerListener = object : IPlayer.PlayerCallback {

        override fun onPlayerReady(exoPlayer: ExoPlayer) {
            if (exoPlayer is SimpleExoPlayer && videoPlayerGLSurfaceView.surfaceTexture != null) {
                exoPlayer.addVideoListener(videoListener)
                exoPlayer.setVideoSurface(Surface(videoPlayerGLSurfaceView.surfaceTexture))
            }
        }

        override fun onMediaChanged(lastUri: Uri?, newUri: Uri) {
            videoPlayerGLSurfaceView.createSurfaceTexture()
        }

        override fun onCompletion(uri: Uri) {
            videoPlayerGLSurfaceView.setHandleCurrentFrame(false)
            videoPlayer.play(uri)
        }

        override fun onPlaybackStateUpdated(playbackState: PlaybackState) {
            when (playbackState.state) {
                PlayerState.STATE_PLAYING -> videoPlayerGLSurfaceView.setHandleCurrentFrame(true)
            }
        }

        override fun onError(message: String) {

        }
    }

    init {
        videoPlayerGLSurfaceView.setOnCreateCallback {
            currentUri?.let { videoPlayer.play(it) }
        }
        videoPlayer.addPlayerListener(playerListener)
    }

    fun play(uri: Uri) {
        currentUri = uri
        if(surfaceTextureCreated)
            videoPlayer.play(uri)
    }

    fun addPlayerListener(playerListener: IPlayer.PlayerCallback) {
        videoPlayer.addPlayerListener(playerListener)
    }

    fun pause() {
        videoPlayer.pause()
    }

    fun stop() {
        videoPlayer.stop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        videoPlayer.addPlayerListener(playerListener)
        videoPlayerGLSurfaceView.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        videoPlayer.release()
        videoPlayerGLSurfaceView.release()
        videoPlayerGLSurfaceView.onPause()
    }
}