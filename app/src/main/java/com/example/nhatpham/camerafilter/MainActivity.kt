package com.example.nhatpham.camerafilter

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.util.DisplayMetrics
import android.util.Log
import com.example.nhatpham.camerafilter.databinding.ActivityMainBinding
import org.wysaid.common.Common
import org.wysaid.myUtils.FileUtil
import org.wysaid.nativePort.CGEFFmpegNativeLibrary
import org.wysaid.nativePort.CGENativeLibrary
import org.wysaid.view.ImageGLSurfaceView
import java.io.IOException
import java.io.InputStream
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    lateinit var mBinding: ActivityMainBinding
    internal lateinit var previewImagesAdapter: PreviewImagesAdapter
    lateinit var selectedPhotoUri: String
    private var mBitmap: Bitmap? = null
    private var mCurrentConfig: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        startActivity(PreviewActivity.newIntent(this, "", false))
        finish()
    }

    private fun checkToRequestReadExternalPermission() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!requestPermissions(this, REQUEST_READ_EXTERNAL_PERMISSIONS, *permissions)) {
//            openPhotoGallery(this, true, OPEN_GALLERY_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_READ_EXTERNAL_PERMISSIONS -> openPhotoGallery(this, true, OPEN_GALLERY_REQUEST_CODE)
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == OPEN_GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                data?.data?.let { photoUri ->
                    requestPersistablePermission(this, data, photoUri)
                    selectedPhotoUri = photoUri.toString()

//                    startActivity(PreviewActivity.newIntent(this, selectedPhotoUri, false))

//                    previewImagesAdapter.imageUri = selectedPhotoUri
//                    previewImagesAdapter.notifyDataSetChanged()
//
//                    try {
//
//                        val stream = contentResolver.openInputStream(photoUri)
//                        val bmp = BitmapFactory.decodeStream(stream)
//
//                        var w = bmp.width
//                        var h = bmp.height
//                        val s = Math.max(w / 2048.0f, h / 2048.0f)
//
//                        if (s > 1.0f) {
//                            w /= s.toInt()
//                            h /= s.toInt()
//                            mBitmap = Bitmap.createScaledBitmap(bmp, w, h, false)
//                        } else {
//                            mBitmap = bmp
//                        }
//
//                        mBinding.imageView.setImageBitmap(mBitmap)
//
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                    Thread {
//                        val outputFilename = FileUtil.getPath() + "/outputVideo.mp4"
//                        val inputFileName = getPath(this, photoUri) ?: return@Thread
//                        CGEFFmpegNativeLibrary.generateVideoWithFilter(outputFilename, inputFileName, "@adjust lut late_sunset.png", 1.0f, null, CGENativeLibrary.TextureBlendMode.CGE_BLEND_ADDREV, 1.0f, false);
//
//                        Log.d("WTF", "done save")
//                        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://$outputFilename")))
//                    }.start()
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }



    private fun openPhotoGallery(activity: Activity, allowMultiPick: Boolean, requestCode: Int) {
        if (Build.VERSION.SDK_INT < 19) {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            activity.startActivityForResult(intent, requestCode)
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiPick)
            intent.type = "image/*"
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private fun requestPersistablePermission(activity: Activity, data: Intent, uri: Uri?) {
        if (Build.VERSION.SDK_INT >= 19) {
            val takeFlags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            try {
                if (uri != null) {
                    activity.contentResolver
                            .takePersistableUriPermission(uri, takeFlags)
                } else {
                    //todo notify user something wrong with selected photo
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun convertDpToPixel(context: Context, dp: Float): Int {
        val displayMetrics = context.resources.displayMetrics
        return Math.round(dp * (displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT))
    }

    companion object {
        private const val SELECTED_PHOTOS = "selected-photos"
        private const val REQUEST_READ_EXTERNAL_PERMISSIONS = 1
        private const val OPEN_GALLERY_REQUEST_CODE = 1

    }


    fun getPath(context: Context, uri: Uri): String? {
        // DocumentProvider
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {

                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)

        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                      selectionArgs: Array<String>?): String? {

        var cursor: Cursor? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)

        try {
            cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                return cursor.getString(index)
            }
        } finally {
            if (cursor != null)
                cursor.close()
        }
        return null
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri) = "com.android.externalstorage.documents" == uri.authority

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri) = "com.android.providers.downloads.documents" == uri.authority

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri) = "com.android.providers.media.documents" == uri.authority

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri) = "com.google.android.apps.photos.content" == uri.authority
}
