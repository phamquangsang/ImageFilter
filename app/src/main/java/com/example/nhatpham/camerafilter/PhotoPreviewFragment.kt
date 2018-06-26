package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.nhatpham.camerafilter.databinding.FragmentPhotoPreviewBinding
import org.wysaid.view.ImageGLSurfaceView
import android.support.design.widget.BottomSheetBehavior



class PhotoPreviewFragment : Fragment() {

    private lateinit var mBinding: FragmentPhotoPreviewBinding
    private val photoUri: String by lazy {
        arguments?.getString(EXTRA_PHOTO_URI) ?: ""
    }
    private var currentBitmap: Bitmap? = null
    private var currentConfig: String? = null
    private val imageConfigsFragment : ImageConfigsFragment by lazy {
        ImageConfigsFragment.newInstance(photoUri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_preview, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mBinding.imageView.displayMode = ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FILL
        mBinding.imageView.setSurfaceCreatedCallback {
            mBinding.imageView.setImageBitmap(currentBitmap)
            mBinding.imageView.setFilterWithConfig(currentConfig)
        }

        imageConfigsFragment.configChangeListener = object : ImageConfigsFragment.ConfigChangeListener {
            override fun onFilterChanged(config: String) {
                mBinding.imageView.post {
                    currentConfig = config
                    mBinding.imageView.setFilterWithConfig(config)
                }
            }

            override fun onBrightnessChanged(value: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSaturationChanged(value: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onClosed() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        Handler().postDelayed({
            childFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.translation_up, R.anim.translation_down)
                    .add(R.id.layoutControl, imageConfigsFragment)
                    .commit()
        }, 1000)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        Glide.with(this)
                .asBitmap()
                .load(photoUri)
                .listener(object : RequestListener<Bitmap>{
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        currentBitmap = resource
                        mBinding.imageView.setFilterWithConfig(currentConfig)
                        mBinding.imageView.setImageBitmap(currentBitmap)
                        return false
                    }
                }).submit()
    }

    override fun onPause() {
        super.onPause()
        mBinding.imageView.release()
        mBinding.imageView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mBinding.imageView.onResume()
    }
    
    companion object {
        private const val EXTRA_PHOTO_URI = "EXTRA_PHOTO_URI"
        
        fun newInstance(photoUri: String): PhotoPreviewFragment {
            return PhotoPreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_PHOTO_URI, photoUri)
                }
            }
        }
    }
}