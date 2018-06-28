package com.example.nhatpham.camerafilter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import com.example.nhatpham.camerafilter.databinding.FragmentCameraBinding
import kotlinx.android.synthetic.main.fragment_camera.*
import org.wysaid.camera.CameraInstance
import org.wysaid.myUtils.FileUtil
import org.wysaid.myUtils.ImageUtil
import org.wysaid.view.CameraRecordGLSurfaceView
import java.io.File

class CameraFragment : Fragment() {

    private lateinit var mBinding: FragmentCameraBinding
    private lateinit var viewModel: PreviewViewModel

    private lateinit var previewImagesAdapter: PreviewImagesAdapter
    private var currentConfig: String? = null
    private val mainHandler = Handler()
    private var isRecording = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        viewModel = ViewModelProviders.of(activity!!).get(PreviewViewModel::class.java)
        mBinding.rcImgPreview.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        previewImagesAdapter = PreviewImagesAdapter(MainActivity.EFFECT_CONFIGS.asList(), object : PreviewImagesAdapter.OnItemInteractListener {

            override fun onConfigSelected(selectedConfig: String) {
                currentConfig = selectedConfig
                mBinding.cameraView.setFilterWithConfig(selectedConfig)
            }
        })
        previewImagesAdapter.imageUri = ""
        mBinding.rcImgPreview.adapter = previewImagesAdapter

        mBinding.btnTakePhoto.setOnClickListener {
            if(!isRecording) {
                mBinding.cameraView.takePicture({ bitmap ->
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
        }

        mBinding.btnTakePhoto.setOnLongClickListener(RecordListener())

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
            }
        }

        mBinding.cameraView.apply {
            setPictureSize(2048, 2048, true) // > 4MP
            setZOrderOnTop(false)
            setZOrderMediaOverlay(true)
        }
    }

    override fun onPause() {
        super.onPause()
        CameraInstance.getInstance().stopCamera()
        mBinding.cameraView.release(null)
        mBinding.cameraView.onPause()
    }

    override fun onResume() {
        super.onResume()
        mBinding.cameraView.onResume()
    }

    inner class RecordListener : View.OnLongClickListener {

        private var isValid = true
        private var recordFilename: String = ""

        override fun onLongClick(v: View?): Boolean {
            val btn = v as Button

            val LOG_TAG = CameraRecordGLSurfaceView.LOG_TAG;

            if (!isValid) {
                Log.e(LOG_TAG, "Please wait for the call...")
                return false
            }

            isValid = false

            if (!mBinding.cameraView.isRecording) {
                btn.text = "Recording"
                Log.i(LOG_TAG, "Start recording...")
                recordFilename = ImageUtil.getPath() + "/rec_" + System.currentTimeMillis() + ".mp4"
                //                recordFilename = ImageUtil.getPath(CameraDemoActivity.this, false) + "/rec_1.mp4";
                mBinding.cameraView.startRecording(recordFilename, { success ->
                    if (success) {
                        mainHandler.post { Toast.makeText(this@CameraFragment.context, "Start recording OK", Toast.LENGTH_SHORT).show() }
                        FileUtil.saveTextContent(recordFilename, FileUtil.getPath() + "/lastVideoPath.txt")
                    } else {
                        mainHandler.post { Toast.makeText(this@CameraFragment.context, "Start recording failed", Toast.LENGTH_SHORT).show() }
                    }
                    isValid = true
                    isRecording = true
                })
            } else {
                mainHandler.post { Toast.makeText(this@CameraFragment.context, "Recorded as: $recordFilename", Toast.LENGTH_SHORT).show() }

                btn.text = "Take Picture"
                Log.i(LOG_TAG, "End recording...")
                mBinding.cameraView.endRecording {
                    Log.i(LOG_TAG, "End recording OK")
                    isValid = true
                    isRecording = false
                }
            }
            return true
        }
    }
}