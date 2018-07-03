package com.example.nhatpham.camerafilter

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
import com.example.nhatpham.camerafilter.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {

    private lateinit var mBinding: FragmentGalleryBinding
    private lateinit var viewModel: PreviewViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_gallery, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        viewModel = ViewModelProviders.of(activity!!).get(PreviewViewModel::class.java)

        mBinding.rcImages.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        mBinding.rcImages.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.gallery_space_item_size)))
        mBinding.rcImages.adapter = ImagesAdapter(getImageThumbnails(), object : ImagesAdapter.OnItemInteractListener{
            override fun onThumbnailSelected(thumbnail: Thumbnail) {
                viewModel.openPhotoPreviewEvent.value = getOriginImageUri(thumbnail.originImageId)
            }
        })
        mBinding.btnBack.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun getImageThumbnails(): List<Thumbnail> {
        val projection = arrayOf(MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.DATA, MediaStore.Images.Thumbnails.IMAGE_ID)
        val cursor = context!!.contentResolver.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Thumbnails._ID} DESC")
        val result = ArrayList<Thumbnail>(cursor.count)
        if (cursor.moveToFirst()) {
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA)
            val originImageIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID)
            do {
                result.add(Thumbnail(cursor.getString(dataColumn), cursor.getInt(originImageIdColumn)))
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
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)))
        }
        cursor.close()
        return null
    }

    private fun getVideoThumbnails(): List<Thumbnail> {
        val projection = arrayOf(MediaStore.Images.Thumbnails._ID, MediaStore.Images.Thumbnails.DATA, MediaStore.Images.Thumbnails.IMAGE_ID)
        val cursor = context!!.contentResolver.query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Thumbnails._ID} DESC")
        val result = ArrayList<Thumbnail>(cursor.count)
        if (cursor.moveToFirst()) {
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA)
            val originImageIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID)
            do {
                result.add(Thumbnail(cursor.getString(dataColumn), cursor.getInt(originImageIdColumn)))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    data class Thumbnail(val uri: String, val originImageId: Int)
}