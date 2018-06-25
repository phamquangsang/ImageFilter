package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nhatpham.camerafilter.databinding.FragmentVideoPreviewBinding

class VideoPreviewFragment : Fragment() {

    lateinit var mBinding: FragmentVideoPreviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_video_preview, container, false)
        return mBinding.root
    }
}