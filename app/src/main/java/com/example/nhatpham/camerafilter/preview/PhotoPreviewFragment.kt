package com.example.nhatpham.camerafilter.preview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
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
import com.example.nhatpham.camerafilter.models.Config
import com.example.nhatpham.camerafilter.models.Photo
import com.example.nhatpham.camerafilter.models.isFromCamera
import com.example.nhatpham.camerafilter.models.isFromGallery
import com.example.nhatpham.camerafilter.utils.*
import org.wysaid.myUtils.ImageUtil
import java.io.File
import java.lang.System.exit


internal class PhotoPreviewFragment : Fragment() {

    private lateinit var mBinding: FragmentPhotoPreviewBinding
    private val photo: Photo? by lazy {
        arguments?.getParcelable(EXTRA_PHOTO) as? Photo
    }
    private val imagePathToSave by lazy {
        "${getPath()}/${generateImageFileName()}"
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var photoPreviewViewModel: PhotoPreviewViewModel
    private lateinit var previewFiltersAdapter: PreviewFiltersAdapter
    private var currentBitmap: Bitmap? = null
    private val currentConfig
        get() = photoPreviewViewModel.currentConfigLiveData.value ?: NONE_CONFIG

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_preview, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)
        photoPreviewViewModel = ViewModelProviders.of(this).get(PhotoPreviewViewModel::class.java)

        photoPreviewViewModel.showFiltersEvent.observe(viewLifecycleOwner, Observer { active ->
            showFilters(active ?: false)
        })

        photoPreviewViewModel.currentConfigLiveData.value = photo?.config ?: NONE_CONFIG
        photoPreviewViewModel.currentConfigLiveData.observe(viewLifecycleOwner, Observer { newConfig ->
            if (newConfig != null) {
                mBinding.imageView.setFilterWithConfig(newConfig.value)
                mBinding.tvFilterName.text = newConfig.name
                previewFiltersAdapter.setNewConfig(newConfig)
            }
        })

        mBinding.imageView.displayMode = ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FILL
        mBinding.imageView.setSurfaceCreatedCallback {
            mBinding.imageView.setImageBitmap(currentBitmap)
            mBinding.imageView.setFilterWithConfig(photoPreviewViewModel.currentConfigLiveData.value?.value)
        }

        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewFiltersAdapter = PreviewFiltersAdapter(context!!, EFFECT_CONFIGS, object : PreviewFiltersAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: Config) {
                if (currentBitmap != null)
                    photoPreviewViewModel.currentConfigLiveData.value = selectedConfig
            }
        })
        previewFiltersAdapter.imageUri = ""
        mBinding.rcImgPreview.adapter = previewFiltersAdapter
        val pos = previewFiltersAdapter.findConfigPos(photo?.config ?: NONE_CONFIG)
        mBinding.rcImgPreview.scrollToPosition(pos ?: 0)

        mBinding.btnPickStickers.setOnClickListener {
            mBinding.btnPickStickers.isSelected = mBinding.btnPickStickers.isSelected.not()
        }

        mBinding.btnPickFilters.setOnClickListener {
            photoPreviewViewModel.showFiltersEvent.value = photoPreviewViewModel.showFiltersEvent.value?.not() ?: true
        }

        mBinding.btnDone.setOnClickListener {
            if (photo == null) {
                exit()
                return@setOnClickListener
            }

            mBinding.imageView.getResultBitmap { bitmap ->
                val currentPhoto = photo
                if (bitmap != null && currentPhoto != null) {
                    val photoUri = currentPhoto.uri
                    if (isMediaStoreImageUri(photoUri) || isFileUri(photoUri) ||
                            URLUtil.isHttpUrl(photoUri.toString()) || URLUtil.isHttpsUrl(photoUri.toString())) {

                        if (currentPhoto.isFromGallery() && currentConfig == NONE_CONFIG) {
                            mainViewModel.doneEditEvent.postValue(currentPhoto.uri)
                        } else {
                            if(isExternalStorageWritable()) {
                                val filePath = ImageUtil.saveBitmap(bitmap, imagePathToSave)
                                if (!filePath.isNullOrEmpty()) {
                                    mainViewModel.doneEditEvent.postValue(Uri.fromFile(File(filePath)).also {
                                        reScanFile(it)
                                    })
                                }
                            }
                        }
                    } else {
                        mainViewModel.doneEditEvent.postValue(null)
                    }
                } else {
                    mainViewModel.doneEditEvent.postValue(null)
                }
            }
        }

        mBinding.btnBack.setOnClickListener { exit() }

        Glide.with(this)
                .asBitmap()
                .load(photo?.uri)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        currentBitmap = resource
                        mBinding.imageView.post {
                            mBinding.imageView.setFilterWithConfig(currentConfig.value)
                        }
                        mBinding.imageView.setImageBitmap(currentBitmap)
                        return false
                    }
                }).submit()
    }

    private fun showFilters(visible: Boolean) {
        val duration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        if (visible) {
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.alpha = 0.6F
                mBinding.rcImgPreview.isVisible = true
            }
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.animate()
                        .alpha(1F)
                        .setDuration(duration)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
            }
            mBinding.tvFilterName.post {
                mBinding.tvFilterName.alpha = 0.6F
                mBinding.tvFilterName.text = currentConfig.name
                mBinding.tvFilterName.isVisible = true
            }
            mBinding.tvFilterName.post {
                mBinding.tvFilterName.animate()
                        .alpha(1F)
                        .setDuration(duration)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
            }
            mBinding.btnPickFilters.isSelected = true
        } else {
            mBinding.rcImgPreview.animate()
                    .alpha(0F)
                    .setDuration(duration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.rcImgPreview.animate().setListener(null)
                            mBinding.rcImgPreview.isVisible = false
                        }
                    }).start()
            mBinding.tvFilterName.animate()
                    .alpha(0F)
                    .setDuration(duration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.tvFilterName.animate().setListener(null)
                            mBinding.tvFilterName.isVisible = false
                        }
                    })
                    .start()
            mBinding.btnPickFilters.isSelected = false
        }
    }

    private fun reScanFile(photoUri: Uri) {
        activity?.let {
            reScanFile(it, photoUri)
        }
    }

    private fun exit() {
        activity?.supportFragmentManager?.popBackStack()
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

    override fun onDestroy() {
        photo?.let {
            if (it.isFromCamera())
                checkToDeleteTempFile(it.uri)
        }
        super.onDestroy()
    }

    private fun checkToDeleteTempFile(uri: Uri) {
        if (isFileUri(uri)) {
            File(uri.path).apply {
                if (exists()) {
                    delete()
                    reScanFile(uri)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PHOTO = "EXTRA_PHOTO"

        fun newInstance(photo: Photo): PhotoPreviewFragment {
            return PhotoPreviewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_PHOTO, photo)
                }
            }
        }
    }
}