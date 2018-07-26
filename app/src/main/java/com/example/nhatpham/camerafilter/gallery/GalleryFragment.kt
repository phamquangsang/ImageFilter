package com.example.nhatpham.camerafilter.gallery

import android.content.ContentUris
import android.databinding.DataBindingUtil
import android.net.Uri
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
import android.view.View.OnLayoutChangeListener


internal class GalleryFragment : Fragment() {

    private lateinit var mBinding: FragmentGalleryBinding
    private lateinit var mainViewModel: MainViewModel
    private lateinit var thumbnailsAdapter: ThumbnailsAdapter
    val lastSelectedItemPos get() = lastSelectedItemPosInternal
    private var lastSelectedItemPosInternal = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_gallery, container, false)
        initialize()
        return mBinding.root
    }

    private fun initialize() {
        mainViewModel = getViewModel(activity!!)

        mBinding.rcImages.layoutManager = GridLayoutManager(context, 3, GridLayoutManager.VERTICAL, false)
        mBinding.rcImages.addItemDecoration(SpacesItemDecoration(resources.getDimensionPixelSize(R.dimen.gallery_space_item_size)))

        thumbnailsAdapter = ThumbnailsAdapter(this, ArrayList(), object : ThumbnailsAdapter.OnItemInteractListener {
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

        mBinding.btnBack.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }

        prepareExitTransitions()
        postponeEnterTransition()
    }

    private fun prepareExitTransitions() {
        val animShortDuration = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        val inflatedExitTransition = TransitionInflater.from(context).inflateTransition(R.transition.grid_exit_fade_transition)
        inflatedExitTransition.duration = animShortDuration
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
                    scrollToPosition()
                }
            }
        }
    }

    /**
     * Scrolls the recycler view to show the last viewed item in the grid. This is important when
     * navigating back from the grid.
     */
    private fun scrollToPosition() {
        mBinding.rcImages.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(v: View,
                                        left: Int,
                                        top: Int,
                                        right: Int,
                                        bottom: Int,
                                        oldLeft: Int,
                                        oldTop: Int,
                                        oldRight: Int,
                                        oldBottom: Int) {
                mBinding.rcImages.removeOnLayoutChangeListener(this)
                val layoutManager = mBinding.rcImages.layoutManager
                val viewAtPosition = layoutManager.findViewByPosition(lastSelectedItemPosInternal)
                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null || layoutManager.isViewPartiallyVisible(viewAtPosition,
                                false, true)) {
                    mBinding.rcImages.post {
                        layoutManager.scrollToPosition(lastSelectedItemPosInternal)
                    }
                }
            }
        })
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

    data class Thumbnail(val id: Long,
                         val uri: String,
                         val isVideo: Boolean = false,
                         val duration: Long = 0)
}