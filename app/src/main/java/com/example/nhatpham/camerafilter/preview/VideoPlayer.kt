package com.example.nhatpham.camerafilter.preview

import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import com.example.nhatpham.camerafilter.BuildConfig
import com.example.nhatpham.camerafilter.STORAGE_DIR_NAME
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.C.CONTENT_TYPE_MUSIC
import com.google.android.exoplayer2.C.USAGE_MEDIA
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

internal class VideoPlayer(val context: Context) : IPlayer {

    private val wifiLock: WifiManager.WifiLock = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL, "academy_audio_lock")

    var exoPlayer: SimpleExoPlayer? = null
        private set

    private val playerCallbacks = ArrayList<IPlayer.PlayerCallback>()
    private var exoPlayerNullIsStopped = false
    private val eventListener = ExoPlayerEventListener()

    var currentUri: Uri? = null
    override val currentState: PlaybackState
        get() {
            val currentPlayer = exoPlayer ?: return if (exoPlayerNullIsStopped)
                PlaybackState(PlayerState.STATE_STOPPED, currentUri)
            else PlaybackState(PlayerState.STATE_NONE, currentUri)

            val state = when (currentPlayer.playbackState) {
                Player.STATE_IDLE -> PlayerState.STATE_PAUSED
                Player.STATE_BUFFERING -> PlayerState.STATE_BUFFERING
                Player.STATE_READY -> if (currentPlayer.playWhenReady) PlayerState.STATE_PLAYING else PlayerState.STATE_PAUSED
                Player.STATE_ENDED -> PlayerState.STATE_PAUSED
                else -> PlayerState.STATE_NONE
            }
            return PlaybackState(state, currentUri)
        }

    override val isPlaying
    get() = with(exoPlayer) { this != null && playWhenReady }

    override fun play(uri: Uri) {
        if (currentUri == null || currentUri != uri || exoPlayer == null) {
            notifyMediaChanged(currentUri, uri)
            currentUri = uri

            releaseResources(false)

            if (exoPlayer == null) {
                exoPlayer = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(context),
                        DefaultTrackSelector(), DefaultLoadControl())
                        .also { player ->
                            player.addListener(eventListener)
                        }
            }

            // Android "O" makes much greater use of AudioAttributes, especially
            // with regards to AudioFocus. All of UAMP's tracks are music, but
            // if your content includes spoken word such as audiobooks or podcasts
            // then the content type should be set to CONTENT_TYPE_SPEECH for those
            // tracks.
            val audioAttributes = AudioAttributes.Builder()
                    .setContentType(CONTENT_TYPE_MUSIC)
                    .setUsage(USAGE_MEDIA)
                    .build()
            exoPlayer?.audioAttributes = audioAttributes

            // Produces DataSource instances through which media data is loaded.
            val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, "CameraFilter"), null)
            // Produces Extractor instances for parsing the media data.
            val extractorsFactory = DefaultExtractorsFactory()
            // The MediaSource represents the media to be played.
            val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
                    .setExtractorsFactory(extractorsFactory)
                    .createMediaSource(uri)

            // Prepares media to play (happens on background thread) and triggers
            // {@code onPlayerStateChanged} callback when the stream is ready to play.
            exoPlayer?.prepare(mediaSource)
            notifyPlayerReady()

            // If we are streaming from the internet, we want to hold a
            // Wifi lock, which prevents the Wifi radio from going to
            // sleep while the song is playing.
            wifiLock.acquire()
        }
        exoPlayer?.playWhenReady = true
    }

    override fun pause() {
        // Pause player and cancel the 'foreground service' state.
        exoPlayer?.playWhenReady = false
        releaseResources(false)
    }

    override fun stop() {
        releaseResources(true)
    }

    override fun addPlayerListener(playerCallback: IPlayer.PlayerCallback) {
        if (!playerCallbacks.contains(playerCallback)) {
            playerCallbacks.add(playerCallback)
        }
    }

    override fun removePlayerListener(playerCallback: IPlayer.PlayerCallback) {
        playerCallbacks.remove(playerCallback)
    }

    override fun removeAllListeners() {
        playerCallbacks.clear()
    }

    override fun release() {
        removeAllListeners()
        releaseResources(true)
    }

    private fun releaseResources(releasePlayer: Boolean) {
        val currentPlayer = exoPlayer
        if (releasePlayer && currentPlayer != null) {
            currentPlayer.release()
            currentPlayer.removeListener(null)
            exoPlayer = null
            exoPlayerNullIsStopped = true
            // mPlayOnFocusGain = false
        }
        if (wifiLock.isHeld) {
            wifiLock.release()
        }
    }

    private inner class ExoPlayerEventListener : Player.EventListener {

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

        }

        override fun onLoadingChanged(isLoading: Boolean) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    stop()
                    currentUri?.let { notifyComplete(it) }
                } else -> {
                    notifyStateChanged(currentState)
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {

        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            val what: String
            if (error != null) {
                what = when (error.type) {
                    ExoPlaybackException.TYPE_SOURCE -> error.sourceException.message.toString()
                    ExoPlaybackException.TYPE_RENDERER -> error.rendererException.message.toString()
                    ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.message.toString()
                    else -> "Unknown: $error"
                }
                notifyError("ExoPlayer error $what")
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {

        }

        override fun onSeekProcessed() {

        }

        override fun onPositionDiscontinuity(reason: Int) {

        }
    }

    private fun notifyPlayerReady() {
        exoPlayer?.let {
            for (callback in playerCallbacks) {
                callback.onPlayerReady(it)
            }
        }
    }

    private fun notifyError(message: String) {
        for (callback in playerCallbacks) {
            callback.onError(message)
        }
    }

    private fun notifyStateChanged(newState: PlaybackState) {
        for (callback in playerCallbacks) {
            callback.onPlaybackStateUpdated(newState)
        }
    }

    private fun notifyComplete(uri: Uri) {
        for (callback in playerCallbacks) {
            callback.onCompletion(uri)
        }
    }

    private fun notifyMediaChanged(oldUri: Uri?, newUri: Uri) {
        for (callback in playerCallbacks) {
            callback.onMediaChanged(oldUri, newUri)
        }
    }
}