package com.example.nhatpham.camerafilter

import android.Manifest
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.nhatpham.camerafilter.databinding.ActivityPreviewBinding

class PreviewActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityPreviewBinding
    private lateinit var viewModel: PreviewViewModel
    private val cameraFragment by lazy { CameraFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        viewModel = ViewModelProviders.of(this).get(PreviewViewModel::class.java)
        viewModel.openPreviewEvent.observe(this, Observer { mediaFilePath ->
            mediaFilePath?.run {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, PhotoPreviewFragment.newInstance(this))
                        .addToBackStack(null)
                        .commit()
            }
        })
        checkToRequestPermissions()
    }

    private fun checkToRequestPermissions() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!requestPermissions(this, REQUEST_USE_CAMERA_PERMISSIONS, *permissions)) {
            showCameraFragment()
        }
    }

    private fun showCameraFragment() {
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, cameraFragment)
                .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_USE_CAMERA_PERMISSIONS -> showCameraFragment()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        const val REQUEST_USE_CAMERA_PERMISSIONS = 1

        fun newIntent(context: Context, photoUri: String, isVideo: Boolean) = Intent(context, PreviewActivity::class.java)
                .apply {
            putExtra("nhat", photoUri)
        }
    }
}
