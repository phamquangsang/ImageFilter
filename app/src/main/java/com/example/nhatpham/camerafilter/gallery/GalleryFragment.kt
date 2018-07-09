package com.example.nhatpham.camerafilter.gallery

import android.arch.lifecycle.ViewModelProviders
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
import com.example.nhatpham.camerafilter.MainViewModel
import com.example.nhatpham.camerafilter.R
import com.example.nhatpham.camerafilter.SpacesItemDecoration
import com.example.nhatpham.camerafilter.databinding.FragmentGalleryBinding
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
        mainViewModel = ViewModelProviders.of(activity!!).get(MainViewModel::class.java)

        mBinding.rcImages.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        mBinding.rcImages.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.gallery_space_item_size)))

        val thumbnails = getImageThumbnails().also {
            it.addAll(0, getVideoThumbnails())
        }.also {
            it.sortWith(Comparator { o1, o2 -> (o2.id - o1.id).toInt() })
        }
        mBinding.rcImages.adapter = ImagesAdapter(thumbnails, object : ImagesAdapter.OnItemInteractListener {
            override fun onThumbnailSelected(thumbnail: Thumbnail) {
                if (thumbnail.isVideo)
                    mainViewModel.openVideoPreviewEvent.value = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, thumbnail.id)
                else
                    mainViewModel.openPhotoPreviewEvent.value = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, thumbnail.id)
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