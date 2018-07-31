package com.example.nhatpham.camerafilter.custom

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.View

/**
 * Fragment providing separate lifecycle owners for each created view hierarchy.
 *
 *
 * This is one possible way to solve issue https://github.com/googlesamples/android-architecture-components/issues/47
 *
 * @author Christophe Beyls
 */
internal open class ViewLifecycleFragment : Fragment() {

    class ViewLifecycleOwner : LifecycleOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)

        override fun getLifecycle() = lifecycleRegistry
    }

    protected var viewLifecycleOwner: ViewLifecycleOwner? = null
    private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner = ViewLifecycleOwner()
        viewLifecycleOwner!!.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStart() {
        super.onStart()
        if (viewLifecycleOwner != null) {
            viewLifecycleOwner!!.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewLifecycleOwner != null) {
            viewLifecycleOwner!!.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    override fun onPause() {
        if (viewLifecycleOwner != null) {
            viewLifecycleOwner!!.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        super.onPause()
    }

    override fun onStop() {
        if (viewLifecycleOwner != null) {
            viewLifecycleOwner!!.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        super.onStop()
    }

    override fun onDestroyView() {
        if (viewLifecycleOwner != null) {
            viewLifecycleOwner!!.lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            viewLifecycleOwner = null
        }
        super.onDestroyView()
    }
}