package com.example.nhatpham.camerafilter.preview

import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer

internal interface IPlayer {

    val isPlaying: Boolean

    val currentState: PlaybackState

    fun play(uri: Uri)

    fun pause()

    fun stop()

    fun addPlayerListener(playerCallback: PlayerCallback)

    fun removePlayerListener(playerCallback: PlayerCallback)

    fun removeAllListeners()

    fun release()

    interface PlayerCallback {

        fun onPlayerReady(exoPlayer: ExoPlayer)

        fun onMediaChanged(lastUri: Uri?, newUri: Uri)

        fun onCompletion(uri: Uri)

        fun onPlaybackStateUpdated(playbackState: PlaybackState)

        fun onError(message: String)
    }

    abstract class SimplePlayerCallback : PlayerCallback {

        override fun onPlayerReady(exoPlayer: ExoPlayer) {

        }

        override fun onMediaChanged(lastUri: Uri?, newUri: Uri) {

        }

        override fun onCompletion(uri: Uri) {

        }

        override fun onPlaybackStateUpdated(playbackState: PlaybackState) {

        }

        override fun onError(message: String) {

        }
    }
}

internal enum class PlayerState {
    STATE_NONE,
    STATE_STOPPED,
    STATE_PLAYING,
    STATE_PAUSED,
    STATE_BUFFERING
}

internal data class PlaybackState(val state: PlayerState, val uri: Uri?)