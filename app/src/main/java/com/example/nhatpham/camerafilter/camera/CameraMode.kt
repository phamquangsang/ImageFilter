package com.example.nhatpham.camerafilter.camera

internal sealed class CameraMode {
    object Photo : CameraMode() {
        override fun toString(): String {
            return "Photo"
        }
    }
    object Video : CameraMode() {
        override fun toString(): String {
            return "Video"
        }
    }
}