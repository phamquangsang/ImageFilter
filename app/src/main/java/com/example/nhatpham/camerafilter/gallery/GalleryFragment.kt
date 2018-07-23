package com.example.nhatpham.camerafilter.gallery

import android.content.ContentUris
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.provider.MediaStore
import android.support.v7.widget.GridLayoutManager
import com.example.nhatpham.camerafilter.*
import com.example.nhatpham.camerafilter.databinding.FragmentGalleryBinding
import com.example.nhatpham.camerafilter.models.Photo
import com.example.nhatpham.camerafilter.models.Source
import com.example.nhatpham.camerafilter.models.Video
import com.example.nhatpham.camerafilter.utils.getViewModel
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.Comparator
import kotlin.collections.ArrayList

internal class GalleryFragment : Fragment() {

    private lateinit var mBinding: FragmentGalleryBinding
    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_gallery, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mainViewModel = getViewModel(activity!!)

        mBinding.rcImages.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        mBinding.rcImages.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.gallery_space_item_size)))


        doAsync {
            val thumbnails = ArrayList<Thumbnail>().apply {
                when (PREVIEW_TYPE) {
                    PreviewType.Photo -> addAll(getImageThumbnails())
                    PreviewType.Video -> addAll(getVideoThumbnails())
                    else -> {
                        addAll(getImageThumbnails())
                        addAll(getVideoThumbnails())
                    }
                }
                sortWith(Comparator { o1, o2 -> (o2.id - o1.id).toInt() })
            }
            uiThread {
                val adapter = mBinding.rcImages.adapter
                if(adapter is ThumbnailsAdapter) {
                    adapter.setThumbnails(thumbnails)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        mBinding.rcImages.adapter = ThumbnailsAdapter(ArrayList(), object : ThumbnailsAdapter.OnItemInteractListener {
            override fun onThumbnailSelected(thumbnail: Thumbnail) {
                if (thumbnail.isVideo) {
                    val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, thumbnail.id)
                    mainViewModel.openVideoPreviewEvent.value = Video(videoUri, NONE_CONFIG, Source.GALLERY)
                } else {
                    val photoUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, thumbnail.id)
                    mainViewModel.openPhotoPreviewEvent.value = Photo(photoUri, NONE_CONFIG, Source.GALLERY)
                }
            }
        })
        mBinding.btnBack.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun getImageThumbnails(): ArrayList<Thumbnail> {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA)
        val cursor = context!!.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DEFAULT_SORT_ORDER)
        val result = ArrayList<Thumbnail>(cursor.count)
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            do {
                result.add(Thumbnail(cursor.getLong(idColumn), cursor.getString(dataColumn)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    private fun getOriginImageUri(id: Int): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA)
        val selection = "${MediaStore.Images.Media._ID} = ?"
        val cursor = context!!.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf("$id"),
                null)
        if (cursor.moveToFirst()) {
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)))
        }
        cursor.close()
        return null
    }

    private fun getVideoThumbnails(): ArrayList<Thumbnail> {
        val projection = arrayOf(MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA, MediaStore.Video.Media.DURATION)
        val cursor = context!!.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Video.Media.DEFAULT_SORT_ORDER)
        val result = ArrayList<Thumbnail>(cursor.count)
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            do {
                result.add(Thumbnail(cursor.getLong(idColumn), cursor.getString(dataColumn), true, cursor.getLong(durationColumn)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    private fun getOriginVideoUri(id: Int): Uri? {
        val projection = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA)
        val selection = "${MediaStore.Video.Media._ID} = ?"
        val cursor = context!!.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                arrayOf("$id"),
                null)
        if (cursor.moveToFirst()) {
            return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)))
        }
        cursor.close()
        return null
    }

    data class Thumbnail(val id: Long,
                         val uri: String,
                         val isVideo: Boolean = false,
                         val duration: Long = 0)
}