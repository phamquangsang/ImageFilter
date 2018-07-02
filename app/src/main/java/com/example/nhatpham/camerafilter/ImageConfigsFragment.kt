package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nhatpham.camerafilter.databinding.FragmentImageConfigBinding

class ImageConfigsFragment : Fragment() {

    private lateinit var mBinding : FragmentImageConfigBinding
    private lateinit var previewImagesAdapter: PreviewImagesAdapter
    private val photoUri: String by lazy {
        arguments?.getString(EXTRA_URI) ?: ""
    }

    var configChangeListener : ConfigChangeListener? = null
    var cancelable : Boolean = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_image_config, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mBinding.tvEdit.setOnClickListener {
            mBinding.rcConfigs.visibility = View.VISIBLE
            mBinding.rcImgPreview.visibility = View.GONE
        }
        mBinding.tvFilter.setOnClickListener {
            mBinding.rcConfigs.visibility = View.GONE
            mBinding.rcImgPreview.visibility = View.VISIBLE
        }

        if(cancelable) {
            mBinding.imgClose.visibility = View.VISIBLE
            mBinding.imgClose.setOnClickListener {
                configChangeListener?.onClosed()
            }
        } else {
            mBinding.imgClose.visibility = View.GONE
        }

        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewImagesAdapter = PreviewImagesAdapter(context!!, EFFECT_CONFIGS.keys.toList(), object : PreviewImagesAdapter.OnItemInteractListener {
            override fun onConfigSelected(selectedConfig: String) {
                configChangeListener?.onFilterChanged(selectedConfig)
            }
        })
        previewImagesAdapter.imageUri = photoUri
        mBinding.rcImgPreview.adapter = previewImagesAdapter
    }

    companion object {
        private const val EXTRA_URI = "EXTRA_IMAGE_PREVIEW_URI"

        fun newInstance(imagePreviewUri: String): ImageConfigsFragment {
            return ImageConfigsFragment().apply {
                arguments = Bundle().apply {
                    putString(EXTRA_URI, imagePreviewUri)
                }
            }
        }
    }

    interface ConfigChangeListener {

        fun onFilterChanged(config : String)

        fun onBrightnessChanged(value : Int)

        fun onSaturationChanged(value : Int)

        fun onClosed()
    }
}