package com.example.nhatpham.camerafilter.gallery

import android.content.ContentUris
import android.databinding.DataBindingUtil
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.provider.MediaStore
import android.support.transition.TransitionInflater
import android.support.transition.TransitionSet
import android.support.v4.app.SharedElementCallback
import android.support.v7.widget.GridLayoutManager
import com.example.nhatpham.camerafilter.*
import com.example.nhatpham.camerafilter.custom.SpacesItemDecoration
import com.example.nhatpham.camerafilter.databinding.FragmentGalleryBinding
import com.example.nhatpham.camerafilter.models.Photo
import com.example.nhatpham.camerafilter.models.Source
import com.example.nhatpham.camerafilter.models.Video
import com.example.nhatpham.camerafilter.preview.PhotoReviewFragment
import com.example.nhatpham.camerafilter.preview.VideoReviewFragment
import com.example.nhatpham.camerafilter.utils.getViewModel
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.Comparator
import kotlin.collections.ArrayList
import com.example.nhatpham.camerafilter.models.Thumbnail
import com.example.nhatpham.camerafilter.utils.clickWithDebounce


internal class GalleryFragment : Fragment() {

    private lateinit var mBinding: FragmentGalleryBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var thumbnailsAdapter: ThumbnailsAdapter
    val lastSelectedItemPos get() = lastSelectedItemPosInternal
    private var lastSelectedItemPosInternal = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_gallery, container, false)
        initUI()
        return mBinding.root
    }

    private fun initUI() {
        mainViewModel = getViewModel(activity!!)
        mBinding.rcImages.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        mBinding.rcImages.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.gallery_space_item_size)))

        thumbnailsAdapter = ThumbnailsAdapter(this, ArrayList(), object : ThumbnailsAdapter.OnItemInteractListener {
            override var lastClickTime: Long = 0

            override fun onThumbnailSelected(view: View, position: Int) {
                thumbnailsAdapter.getItem(position)?.let { thumbnail ->
                    lastSelectedItemPosInternal = position
                    if (thumbnail.isVideo) {
                        val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, thumbnail.id)
                        showVideoReviewFragment(Video(videoUri, NONE_CONFIG, Source.GALLERY), view)
                    } else {
                        val photoUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, thumbnail.id)
                        showPhotoReviewFragment(Photo(photoUri, NONE_CONFIG, Source.GALLERY), view)
                    }
                }
            }
        })
        mBinding.rcImages.adapter = thumbnailsAdapter
        mBinding.btnBack.clickWithDebounce { activity?.supportFragmentManager?.popBackStack() }

        val textResId = when (PREVIEW_TYPE) {
            PreviewType.Photo -> R.string.gallery_photo_title
            PreviewType.Video -> R.string.gallery_video_title
            else -> R.string.gallery_title
        }
        mBinding.tvGalleryTitle.setText(textResId)

        prepareExitTransitions()
        postponeEnterTransition()
    }

    private fun prepareExitTransitions() {
        val animShortDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        val inflatedExitTransition = TransitionInflater.from(context).inflateTransition(R.transition.grid_exit_fade_transition)
        inflatedExitTransition.duration = animShortDuration
        inflatedExitTransition.excludeTarget(mBinding.layoutBar, true)
        exitTransition = inflatedExitTransition

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>?, sharedElements: MutableMap<String, View>?) {
                // Locate the ViewHolder for the clicked position.
                val selectedViewHolder = mBinding.rcImages.findViewHolderForAdapterPosition(lastSelectedItemPosInternal)
                if (selectedViewHolder?.itemView == null) {
                    return
                }
                // Map the first shared element name to the child ImageView.
                sharedElements!![names!![0]] = selectedViewHolder.itemView.findViewById(R.id.image)
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                if (adapter is ThumbnailsAdapter) {
                    adapter.setThumbnails(thumbnails)
                    adapter.notifyDataSetChanged()
                }
            }
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

    private fun showPhotoReviewFragment(photo: Photo, sharedView: View) {
        activity?.run {
            // The 'view' is the card view that was clicked to initiate the transition.
            (exitTransition as TransitionSet).excludeTarget(sharedView, true)

            supportFragmentManager.beginTransaction()
                    .also {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            it.setReorderingAllowed(true)
                                    .addSharedElement(sharedView, sharedView.transitionName)
                                    .setCustomAnimations(0, 0, 0, 0)
                        }
                    }
                    .hide(this@GalleryFragment)
                    .add(R.id.fragment_container, PhotoReviewFragment.newInstance(photo))
                    .addToBackStack(null)
                    .commit()
        }
    }

    private fun showVideoReviewFragment(video: Video, sharedView: View) {
        activity?.run {
            // The 'view' is the card view that was clicked to initiate the transition.
            (exitTransition as TransitionSet).excludeTarget(sharedView, true)

            supportFragmentManager.beginTransaction()
                    .also {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            it.setReorderingAllowed(true).addSharedElement(sharedView, sharedView.transitionName)
                        }
                    }
                    .hide(this@GalleryFragment)
                    .add(R.id.fragment_container, VideoReviewFragment.newInstance(video))
                    .addToBackStack(null)
                    .commit()
        }
    }
}