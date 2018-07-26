package com.example.nhatpham.camerafilter.preview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.transition.Transition
import android.support.transition.TransitionInflater
import android.support.transition.TransitionListenerAdapter
import android.support.v4.app.SharedElementCallback
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
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
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.*
import com.example.nhatpham.camerafilter.models.*
import com.example.nhatpham.camerafilter.utils.*
import org.wysaid.myUtils.ImageUtil
import java.io.File
import java.lang.System.exit


internal class PhotoReviewFragment : ViewLifecycleFragment(), View.OnClickListener {

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

    private val animShortDuration by lazy {
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
    }
    private var currentBitmap: Bitmap? = null
    private val currentConfig
        get() = photoPreviewViewModel.currentConfigLiveData.value ?: NONE_CONFIG

    private var enterTransitionEnd = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_photo_preview, container, false)
        if(savedInstanceState == null) {
            postponeEnterTransition()
        }
        initUI()
        return mBinding.root
    }

    private fun initUI() {
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

        mBinding.btnPickStickers.setOnClickListener(this)
        mBinding.btnPickFilters.setOnClickListener(this)
        mBinding.btnDone.setOnClickListener(this)
        mBinding.btnBack.setOnClickListener(this)

        lifecycle.addObserver(mBinding.imageView)
        mBinding.imageView.displayMode = ImageGLSurfaceView.DisplayMode.DISPLAY_ASPECT_FILL
        mBinding.imageView.setSurfaceCreatedCallback {
            if(enterTransitionEnd) {
                updateCurrentImage()
            }
        }

        // Only apply shared element transition from gallery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && photo?.isFromGallery() == true) {
            enterTransitionEnd = false
            mBinding.imageViewTemp.isVisible = true

            val photoUri = photo?.uri
            if (photoUri != null)
                mBinding.imageViewTemp.transitionName = photoUri.lastPathSegment
        }
        Glide.with(this)
                .asBitmap()
                .load(photo?.uri)
                .apply(RequestOptions.centerInsideTransform())
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                        startPostponedEnterTransition()
                        return false
                    }

                    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        startPostponedEnterTransition()
                        currentBitmap = resource

                        // we're not applying shared element transition from camera, so update the image now
                        if(photo?.isFromCamera() == true) {
                            mBinding.imageViewTemp.isVisible = false
                            updateCurrentImage()
                        }
                        return false
                    }
                }).into(mBinding.imageViewTemp)
        prepareSharedElementTransition()
    }

    private fun prepareSharedElementTransition() {
        val transition = TransitionInflater.from(context).inflateTransition(R.transition.image_shared_element_transition)
        transition.duration = animShortDuration
        transition.addListener(object : TransitionListenerAdapter() {
            override fun onTransitionEnd(transition: Transition) {
                transition.removeListener(this)
                enterTransitionEnd = true

                mBinding.imageViewTemp.postDelayed({
                    mBinding.imageViewTemp.isInvisible = true
                    updateCurrentImage()
                }, animShortDuration)
            }
        })
        sharedElementEnterTransition = transition

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>?, sharedElements: MutableMap<String, View>?) {
                sharedElements!![names!![0]] = mBinding.imageViewTemp
            }
        })
    }

    private fun updateCurrentImage() {
        mBinding.imageView.setFilterWithConfig(currentConfig.value)
        mBinding.imageView.setImageBitmap(currentBitmap)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainViewModel = getViewModel(activity!!)
        photoPreviewViewModel = getViewModel(this)

        photoPreviewViewModel.showFiltersEvent.observe(viewLifecycleOwner!!, Observer { active ->
            showFilters(active ?: false)
        })

        photoPreviewViewModel.currentConfigLiveData.value = photo?.config ?: NONE_CONFIG
        photoPreviewViewModel.currentConfigLiveData.observe(viewLifecycleOwner!!, Observer { newConfig ->
            if (newConfig != null) {
                mBinding.imageView.setFilterWithConfig(newConfig.value)
                mBinding.tvFilterName.text = newConfig.name
                previewFiltersAdapter.setNewConfig(newConfig)
            }
        })
    }

    private fun showFilters(visible: Boolean) {
        if (visible) {
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.alpha = 0.6F
                mBinding.rcImgPreview.isVisible = true
            }
            mBinding.rcImgPreview.post {
                mBinding.rcImgPreview.animate()
                        .alpha(1F)
                        .setDuration(animShortDuration)
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
                        .setDuration(animShortDuration)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
            }
            mBinding.btnPickFilters.isSelected = true
        } else {
            mBinding.rcImgPreview.animate()
                    .alpha(0F)
                    .setDuration(animShortDuration)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator?) {
                            mBinding.rcImgPreview.animate().setListener(null)
                            mBinding.rcImgPreview.isVisible = false
                        }
                    }).start()
            mBinding.tvFilterName.animate()
                    .alpha(0F)
                    .setDuration(animShortDuration)
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

    override fun onClick(v: View?) {
        when (v) {
            mBinding.btnBack -> exit()
            mBinding.btnDone -> saveImage()
            mBinding.btnPickFilters -> toggleFilters()
            mBinding.btnPickStickers -> toggleStickers()
        }
    }

    private fun exit() {
        activity?.run {
            if (supportFragmentManager.backStackEntryCount == 0)
                finish()
            else supportFragmentManager.popBackStack()
        }
    }

    private fun saveImage() {
        mBinding.imageView.getResultBitmap { bitmap ->
            val currentPhoto = photo
            if (bitmap != null && currentPhoto != null) {
                val photoUri = currentPhoto.uri

                if (currentPhoto.isFromGallery() && currentConfig == NONE_CONFIG) {
                    val path = when {
                        isMediaStoreImageUri(photoUri) -> getPathFromMediaUri(context!!, photoUri)
                        isFileUri(photoUri) -> photoUri.path
                        else -> null
                    }
                    if (path != null)
                        mainViewModel.doneEditEvent.postValue(Uri.parse(path))
                    else
                        mainViewModel.doneEditEvent.postValue(null)
                } else {
                    if (isExternalStorageWritable()) {
                        val filePath = ImageUtil.saveBitmap(bitmap, imagePathToSave)
                        if (!filePath.isNullOrEmpty()) {
                            mainViewModel.doneEditEvent.postValue(Uri.fromFile(File(filePath)).also {
                                reScanFile(it)
                            })
                        } else {
                            Toast.makeText(context, "Cannot save", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                mainViewModel.doneEditEvent.postValue(null)
            }
        }
    }

    private fun reScanFile(photoUri: Uri) {
        activity?.let {
            reScanFile(it, photoUri)
        }
    }

    private fun toggleFilters() {
        photoPreviewViewModel.showFiltersEvent.value = photoPreviewViewModel.showFiltersEvent.value?.not() ?: true
    }

    private fun toggleStickers() {
        mBinding.btnPickStickers.isSelected = mBinding.btnPickStickers.isSelected.not()
    }

    companion object {
        private const val EXTRA_PHOTO = "photo"

        fun newInstance(photo: Photo): PhotoReviewFragment {
            return PhotoReviewFragment().apply {
                arguments = bundleOf(EXTRA_PHOTO to photo)
            }
        }
    }
}