package com.example.nhatpham.camerafilter.models

internal data class Thumbnail(val id: Long) {
    var uri = ""
        private set
    var isVideo = false
        private set
    var duration: Long = 0
        private set

    constructor(id: Long,
                uri: String = "",
                isVideo: Boolean = false,
                duration: Long = 0) : this(id) {
        this.uri = uri
        this.isVideo = isVideo
        this.duration = duration
    }
}