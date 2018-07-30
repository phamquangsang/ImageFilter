package com.example.nhatpham.camerafilter.utils

import android.os.SystemClock
import android.view.View

internal interface IDebounce {
    var lastClickTime: Long
}

internal inline fun <reified T : IDebounce> T.debounce(debounceTime: Long = 500L, action: T.() -> Unit) {
    if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime)
        return
    else action(this)
    lastClickTime = SystemClock.elapsedRealtime()
}

internal fun View.clickWithDebounce(debounceTime: Long = 500L, action: () -> Unit) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime)
                return
            else action()
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

internal fun View.clickWithDebounce(debounceTime: Long = 500L, listener: View.OnClickListener) {
    setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime)
                return
            else {
                lastClickTime = SystemClock.elapsedRealtime()
                listener.onClick(v)
            }
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}