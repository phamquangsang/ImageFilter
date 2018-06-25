package com.example.nhatpham.camerafilter

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nhatpham.camerafilter.databinding.FragmentCameraBinding
import org.wysaid.camera.CameraInstance
import org.wysaid.myUtils.ImageUtil
import java.io.File

class CameraFragment : Fragment() {

    private lateinit var mBinding: FragmentCameraBinding
    private lateinit var viewModel: PreviewViewModel

    private val cameraView by lazy { mBinding.cameraView }
    private var currentConfig: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        viewModel = ViewModelProviders.of(activity!!).get(PreviewViewModel::class.java)

        mBinding.takePhoto.setOnClickListener {
            cameraView.takePicture({ bitmap ->
                if (bitmap != null) {
                    val filePath = ImageUtil.saveBitmap(bitmap)
                    bitmap.recycle()
                    activity?.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(File(filePath))))

                    viewModel.openPreviewEvent.value = filePath

                    Log.d("WTF", "Take Pic success!")
                } else {
                    Log.d("WTF", "Take Pic failed!")
                }
            }, null, currentConfig, 1.0f, true)
        }

        cameraView.apply {
            presetRecordingSize(480, 640)
            setPictureSize(2048, 2048, true) // > 4MP
            setZOrderOnTop(false)
            setZOrderMediaOverlay(true)
        }
    }

    override fun onPause() {
        super.onPause()
        CameraInstance.getInstance().stopCamera()
        cameraView.release(null)
        cameraView.onPause()
    }

    override fun onResume() {
        super.onResume()
        cameraView.onResume()
    }
}