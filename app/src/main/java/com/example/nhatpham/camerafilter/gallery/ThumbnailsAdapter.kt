package com.example.nhatpham.camerafilter.gallery

import android.databinding.DataBindingUtil
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.R
import com.example.nhatpham.camerafilter.databinding.LayoutGalleryItemBinding
import java.util.concurrent.TimeUnit
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import java.util.concurrent.atomic.AtomicBoolean
import com.bumptech.glide.request.target.Target
import com.example.nhatpham.camerafilter.models.Thumbnail
import com.example.nhatpham.camerafilter.utils.IDebounce
import com.example.nhatpham.camerafilter.utils.debounce


internal class ThumbnailsAdapter(private val galleryFragment: GalleryFragment,
                                 thumbnails: List<Thumbnail> = emptyList(),
                                 private val onItemInteractListener: OnItemInteractListener?)
    : RecyclerView.Adapter<ThumbnailsAdapter.ViewHolder>() {

    private val thumbnails: MutableList<Thumbnail> = ArrayList()
    private val requestManager: RequestManager = Glide.with(galleryFragment)
    private val viewHolderListenerImpl = ViewHolderListenerImpl()
    private val requestOptions = RequestOptions().apply {
        diskCacheStrategy(DiskCacheStrategy.ALL)
        frame(100000)
        centerInside()
    }

    init {
        this.thumbnails.addAll(thumbnails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_gallery_item, parent, false)
        return ViewHolder(view, viewHolderListenerImpl)
    }

    override fun getItemCount() = thumbnails.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(thumbnails[position])
    }

    fun getItem(position: Int) = if(thumbnails.isEmpty() || position > itemCount) null else thumbnails[position]

    inner class ViewHolder(itemView: View, val viewHolderListenerImpl: ViewHolderListenerImpl) : RecyclerView.ViewHolder(itemView) {

        private val mBinding = DataBindingUtil.bind<LayoutGalleryItemBinding>(itemView)

        init {
            mBinding!!.root.setOnClickListener {
                onItemInteractListener?.debounce {
                    onThumbnailSelected(mBinding.image, adapterPosition)
                }
            }
        }

        fun bindData(thumbnail: Thumbnail) {
            requestManager
                    .load(thumbnail.uri)
                    .apply(requestOptions)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            viewHolderListenerImpl.onLoadCompleted(mBinding!!.image, adapterPosition)
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            viewHolderListenerImpl.onLoadCompleted(mBinding!!.image, adapterPosition)
                            return false
                        }
                    })
                    .into(mBinding!!.image)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBinding.image.transitionName = thumbnail.id.toString()
            }

            if(thumbnail.isVideo) {
                mBinding.tvDuration.isVisible = true
                mBinding.tvDuration.text = DateUtils.formatElapsedTime(TimeUnit.MILLISECONDS.toSeconds(thumbnail.duration))
                mBinding.imgPlay.isVisible = true
            } else {
                mBinding.tvDuration.isVisible = false
                mBinding.imgPlay.isVisible = false
            }
        }
    }

    fun setThumbnails(thumbnails: List<Thumbnail>) {
        this.thumbnails.apply {
            clear()
            addAll(thumbnails)
        }
    }

    inner class ViewHolderListenerImpl: ViewHolderListener {

        private val enterTransitionStarted: AtomicBoolean = AtomicBoolean()

        override fun onLoadCompleted(view: ImageView, position: Int) {
            if(position != galleryFragment.lastSelectedItemPos) {
                return
            }

            if (enterTransitionStarted.getAndSet(true)) {
                return
            }
            galleryFragment.startPostponedEnterTransition()
        }
    }

    interface OnItemInteractListener : IDebounce {

        fun onThumbnailSelected(view: View, position: Int)
    }

    /**
     * A listener that is attached to all ViewHolders to handle image loading events and clicks.
     */
    private interface ViewHolderListener {

        fun onLoadCompleted(view: ImageView, adapterPosition: Int)
    }
}