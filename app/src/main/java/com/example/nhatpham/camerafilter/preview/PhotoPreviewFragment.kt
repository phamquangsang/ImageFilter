package com.example.nhatpham.camerafilter.preview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.net.Uri
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
import android.webkit.URLUtil
import androidx.core.view.isVisible
import com.example.nhatpham.camerafilter.*
import org.wysaid.myUtils.ImageUtil
import java.io.File


internal class PhotoPreviewFragment : Fragment() {

    private lateinit var mBinding: FragmentPhotoPreviewBinding
    private val photoUri: Uri? by lazy {
        arguments?.getParcelable(EXTRA_PHOTO_URI) as? Uri
    }
    private val fromCamera: Boolean by lazy {
        arguments?.getBoolean(EXTRA_FROM_CAMERA) ?: false
    }
    private val imagePathToSave by lazy {
        "${getPath()}/${generateImageFileName()}"
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var previewFiltersAdapter: PreviewFiltersAdapter
    private var currentBitmap: Bitmap? = null
    private var currentConfig: Config? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_preview, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)

        mBinding.imageView.displayMode = ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FILL
        mBinding.imageView.setSurfaceCreatedCallback {
            mBinding.imageView.setImageBitmap(currentBitmap)
            mBinding.imageView.setFilterWithConfig(currentConfig?.value)
        }

        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewFiltersAdapter = PreviewFiltersAdapter(context!!, EFFECT_CONFIGS, object : PreviewFiltersAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: Config) {
                currentConfig = selectedConfig
                mBinding.imageView.setFilterWithConfig(selectedConfig.value)
            }
        })

        previewFiltersAdapter.imageUri = ""
        mBinding.rcImgPreview.adapter = previewFiltersAdapter

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
            mBinding.imageView.getResultBitmap { bitmap ->
                if (bitmap != null) {
                    if (isMediaStoreImageUri(photoUri)) {
                        if (currentConfig != null) {
                            val photoUri = Uri.fromFile(File(imagePathToSave))
                            ImageUtil.saveBitmap(bitmap, imagePathToSave)
                            activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photoUri))
                            mainViewModel.doneEditEvent.postValue(photoUri)
                        } else {
                            mainViewModel.doneEditEvent.postValue(photoUri)
                        }
                    } else if (isFileUri(photoUri)) {
                        if (currentConfig != null) {
                            ImageUtil.saveBitmap(bitmap, photoUri!!.path)
                            activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photoUri!!))
                            mainViewModel.doneEditEvent.postValue(photoUri)
                        } else {
                            mainViewModel.doneEditEvent.postValue(photoUri)
                        }
                    } else if (URLUtil.isHttpUrl(photoUri.toString()) || URLUtil.isHttpsUrl(photoUri.toString())) {
                        val photoUri = Uri.fromFile(File(imagePathToSave))
                        ImageUtil.saveBitmap(bitmap, imagePathToSave)
                        activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photoUri))
                        mainViewModel.doneEditEvent.postValue(photoUri)
                    } else {
                        mainViewModel.doneEditEvent.postValue(null)
                    }
                } else {
                    mainViewModel.doneEditEvent.postValue(null)
                }
            }
        }

        mBinding.btnBack.setOnClickListener {
            if (fromCamera && isFileUri(photoUri)) {
                File(photoUri!!.path).apply {
                    if (exists()) {
                        delete()
                        activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photoUri))
                    }
                }
            }
            activity?.supportFragmentManager?.popBackStack()
        }

        Glide.with(this)
                .asBitmap()
                .load(photoUri)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        currentBitmap = resource
                        mBinding.imageView.setFilterWithConfig(currentConfig?.value)
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
        private const val EXTRA_FROM_CAMERA = "EXTRA_FROM_CAMERA"

        fun newInstance(photoUri: Uri, fromCamera: Boolean): PhotoPreviewFragment {
            return PhotoPreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_PHOTO_URI, photoUri)
                    putBoolean(EXTRA_FROM_CAMERA, fromCamera)
                }
            }
        }
    }
}