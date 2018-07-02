package com.example.nhatpham.camerafilter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.os.Bundle
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
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isVisible
import java.io.File


class PhotoPreviewFragment : Fragment() {

    private lateinit var mBinding: FragmentPhotoPreviewBinding
    private val photoUri: String by lazy {
        arguments?.getString(EXTRA_PHOTO_URI) ?: ""
    }

    private lateinit var previewImagesAdapter: PreviewImagesAdapter
    private var currentBitmap: Bitmap? = null
    private var currentConfig: String? = null

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

        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewImagesAdapter = PreviewImagesAdapter(context!!, EFFECT_CONFIGS.keys.toList(), object : PreviewImagesAdapter.OnItemInteractListener {

            override fun onConfigSelected(selectedConfig: String) {
                currentConfig = selectedConfig
                mBinding.imageView.setFilterWithConfig(selectedConfig)
            }
        })
        previewImagesAdapter.imageUri = photoUri
        mBinding.rcImgPreview.adapter = previewImagesAdapter

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
            activity?.supportFragmentManager?.popBackStack()
        }

        mBinding.btnBack.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        Glide.with(this)
                .asBitmap()
                .load(photoUri)
                .listener(object : RequestListener<Bitmap> {
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