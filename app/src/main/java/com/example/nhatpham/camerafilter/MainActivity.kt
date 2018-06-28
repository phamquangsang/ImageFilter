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
    lateinit var previewImagesAdapter: PreviewImagesAdapter
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

        val EFFECT_CONFIGS = arrayOf("", // ASCII art (字符画效果)
                "@beautify face 1 480 640", //Beautify
                "@adjust lut A.jpg",
                "@adjust lut A-2.jpg",
                "@adjust lut A-3.jpg",
                "@adjust lut A-4.jpg",
                "@adjust lut A-5.jpg",
                "@adjust lut A-6.jpg",
                "@adjust lut A-7.jpg",
                "@adjust lut A-8.jpg",
                "@adjust lut A-9.jpg",
                "@adjust lut A-10.jpg",
                "@adjust lut A-11.jpg",
                "@adjust lut A-12.jpg",
                "@adjust lut A-13.jpg",
                "@adjust lut A-14.jpg",
                "@adjust lut A-15.jpg",
                "@adjust lut lookup_amatorka.png",
                "@adjust lut lookup_fgfacolor.png",
                "@adjust lut lookup_lofi.png",
                "@adjust lut lookup_mayfair.png",
                "@adjust lut lookup_miss_etikate.png",
                "@adjust lut lookup_nashville.png",
                "@adjust lut lookup_nguocnang.png",
                "@adjust lut lookup_soft_elegance_1.png",
                "@adjust lut lookup_soft_elegance_2.png",
                "@adjust lut filmstock.png",
                "@adjust lut foggy_night.png",
                "@adjust lut soft_warming.png",
                "#unpack @blur lerp 0.75", //can adjust blur intensity
                "@dynamic wave 0.5",
                "@curve R(0, 0)(149, 145)(255, 255)G(0, 0)(149, 145)(255, 255)B(0, 0)(149, 145)(255, 255) @pixblend colordodge 0.937 0.482 0.835 1 20", //421
                "@adjust saturation 0.7 @pixblend screen 1 0.243 0.69 1 30", //426

                "@curve R(0, 0)(71, 74)(164, 165)(255, 255) @pixblend screen 0.94118 0.29 0.29 1 20", //415
                "@curve R(0, 0)(152, 183)(255, 255)G(0, 0)(161, 133)(255, 255) @pixblend overlay 0.357 0.863 0.882 1 40", //420
                "@curve R(0, 0)(149, 145)(255, 255)G(0, 0)(149, 145)(255, 255)B(0, 0)(149, 145)(255, 255) @pixblend colordodge 0.937 0.482 0.835 1 20", //421

                "@curve R(0, 4)(255, 244)G(0, 0)(255, 255)B(0, 84)(255, 194)", //10
                "@curve R(39, 0)(93, 61)(130, 136)(162, 193)(208, 255)G(41, 0)(92, 61)(128, 133)(164, 197)(200, 250)B(0, 23)(125, 127)(255, 230)", //12

                "@adjust hsv -1 -1 -1 -1 -1 -1", //27
                "@curve R(5, 8)(36, 51)(115, 145)(201, 220)(255, 255)G(6, 9)(67, 83)(169, 190)(255, 255)B(3, 3)(55, 60)(177, 190)(255, 255)", //166
                "@curve R(14, 0)(51, 42)(135, 138)(191, 202)(234, 255)G(11, 6)(78, 77)(178, 185)(242, 250)B(11, 0)(22, 10)(72, 60)(171, 162)(217, 209)(255, 255)", //167
                "@curve R(9, 0)(26, 7)(155, 108)(194, 159)(255, 253)G(9, 0)(50, 19)(218, 194)(255, 255)B(0, 0)(29, 9)(162, 116)(218, 194)(255, 255)", //168
                "@curve R(2, 2)(16, 30)(72, 112)(135, 185)(252, 255)G(2, 1)(30, 42)(55, 84)(157, 207)(238, 249)B(1, 0)(26, 17)(67, 106)(114, 165)(231, 250)", //170
                "@curve R(9, 0)(49, 62)(124, 155)(218, 255)G(10, 0)(30, 33)(137, 169)(223, 255)B(10, 0)(37, 45)(96, 122)(150, 182)(221, 255)", //173
                "@curve R(81, 3)(161, 129)(232, 253)G(91, 0)(164, 136)(255, 225)B(76, 0)(196, 162)(255, 225)", //174
                "@vigblend mix 10 10 30 255 91 0 1.0 0.5 0.5 3 @curve R(0, 31)(35, 75)(81, 139)(109, 174)(148, 207)(255, 255)G(0, 24)(59, 88)(105, 146)(130, 171)(145, 187)(180, 214)(255, 255)B(0, 96)(63, 130)(103, 157)(169, 194)(255, 255)")
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
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
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
    fun getDataColumn(context: Context, uri: Uri?, selection: String?,
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
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}
