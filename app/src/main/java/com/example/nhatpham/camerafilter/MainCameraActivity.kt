package com.example.nhatpham.camerafilter

import android.Manifest
import android.app.Activity
import android.app.Application
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.camera.CameraFragment
import com.example.nhatpham.camerafilter.gallery.GalleryFragment
import com.example.nhatpham.camerafilter.models.Config
import com.example.nhatpham.camerafilter.models.Photo
import com.example.nhatpham.camerafilter.models.Source
import com.example.nhatpham.camerafilter.models.Video
import com.example.nhatpham.camerafilter.preview.PhotoReviewFragment
import com.example.nhatpham.camerafilter.preview.VideoReviewFragment
import com.example.nhatpham.camerafilter.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.uiThread
import org.wysaid.common.Common
import org.wysaid.nativePort.CGENativeLibrary
import java.io.*
import java.lang.Exception
import java.net.URL
import kotlin.math.absoluteValue

class MainCameraActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel
    private val currentFragment : Fragment
    get() = supportFragmentManager.findFragmentById(R.id.fragment_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_preview)

        CGENativeLibrary.setLoadImageCallback(object : CGENativeLibrary.LoadImageCallback {
            override fun loadImage(name: String, arg: Any?): Bitmap? {
                Log.i(Common.LOG_TAG, "Loading file: $name")
                val am = assets
                val inputStream: InputStream
                try {
                    inputStream = am.open(name)
                } catch (e: IOException) {
                    Log.e(Common.LOG_TAG, "Can not open file $name")
                    return null
                }
                return BitmapFactory.decodeStream(inputStream)
            }

            override fun loadImageOK(bmp: Bitmap, arg: Any?) {
                Log.i(Common.LOG_TAG, "Loading bitmap over, you can choose to recycle or cache")
                bmp.recycle()
            }
        }, null)

        mainViewModel = getViewModel(this)
        mainViewModel.openPhotoPreviewEvent.observe(this, Observer { photo ->
            if (photo != null) {
                showPhotoReviewFragment(photo, photo.source != Source.NONE)
            }
        })
        mainViewModel.openVideoPreviewEvent.observe(this, Observer { video ->
            if (video != null) {
                showVideoReviewFragment(video, video.source != Source.NONE)
            }
        })
        mainViewModel.openGalleryEvent.observe(this, Observer {
            showGalleryFragment()
        })
        mainViewModel.doneEditEvent.observe(this, Observer {
            setResult(Activity.RESULT_OK, Intent().apply { data = it })
            finish()
        })

        val am = assets
        val inputStream: InputStream
        try {
            inputStream = am.open("filters.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val builder = StringBuilder()
            reader.use { bufferedReader ->
                while (true) {
                    val line = bufferedReader.readLine()
                    if (line != null) {
                        builder.append(line)
                    } else {
                        break
                    }
                }
            }
            EFFECT_CONFIGS.apply {
                clear()
                add(NONE_CONFIG)
                addAll(Gson().fromJson<ArrayList<Config>>(builder.toString(), object : TypeToken<ArrayList<Config>>() {}.type))
            }
        } catch (e: IOException) {
            Log.e(Common.LOG_TAG, "Can not open file filters.json")
        }

        if (!checkToRequestPermissions()) {
            if (savedInstanceState == null) {
                checkToShowUI()
            }
        }
    }

    private fun checkToShowUI() {
        STORAGE_DIR_NAME = intent.getStringExtra(EXTRA_STORAGE_DIR_NAME) ?: ""

        if (intent.data != null) {
            val isVideo = intent.getBooleanExtra(EXTRA_MEDIA_VIDEO, false)
            if (isVideo)
                mainViewModel.openVideoPreviewEvent.value = Video(intent.data, NONE_CONFIG)
            else
                mainViewModel.openPhotoPreviewEvent.value = Photo(intent.data, NONE_CONFIG)
        } else {
            PREVIEW_TYPE = intent.getSerializableExtra(EXTRA_PREVIEW_TYPE) as? PreviewType ?: PreviewType.Both
            showCameraFragment()
        }
    }

    private fun checkToRequestPermissions(): Boolean {
        val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return requestPermissions(this, REQUEST_PERMISSIONS, *permissions)
    }

    private fun showCameraFragment() {
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, CameraFragment())
                .commitAllowingStateLoss()
    }

    private fun showPhotoReviewFragment(photo: Photo, shouldAddToBackStack: Boolean = true) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PhotoReviewFragment.newInstance(photo))
                .also { if (shouldAddToBackStack) it.addToBackStack(null) }
                .commitAllowingStateLoss()
    }

    private fun showVideoReviewFragment(video: Video, shouldAddToBackStack: Boolean = true) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, VideoReviewFragment.newInstance(video))
                .also { if (shouldAddToBackStack) it.addToBackStack(null) }
                .commitAllowingStateLoss()
    }

    private fun showGalleryFragment() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GalleryFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                val somePermissionsNotGranted = grantResults.any { it.absoluteValue != PackageManager.PERMISSION_GRANTED }
                if (!somePermissionsNotGranted) {
                    checkToShowUI()
                } else {
                    finish()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1
        private const val EXTRA_STORAGE_DIR_NAME = "storage_dir_name"
        private const val EXTRA_PREVIEW_TYPE = "preview_type"
        private const val EXTRA_MEDIA_VIDEO = "media_video"

        @JvmOverloads
        @JvmStatic
        fun newIntent(context: Context, storageDirName: String = "", mediaUri: Uri, isVideo: Boolean) =
                context.intentFor<MainCameraActivity>(EXTRA_STORAGE_DIR_NAME to storageDirName,
                        EXTRA_MEDIA_VIDEO to isVideo)
                        .setData(mediaUri)

        @JvmOverloads
        @JvmStatic
        fun newIntent(context: Context, storageDirName: String = "", previewType: PreviewType) =
                context.intentFor<MainCameraActivity>(EXTRA_STORAGE_DIR_NAME to storageDirName,
                        EXTRA_PREVIEW_TYPE to previewType)
    }
}
