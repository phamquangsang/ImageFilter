package com.example.nhatpham.camerafilter.gallery

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.R
import com.example.nhatpham.camerafilter.databinding.LayoutGalleryItemBinding
import java.util.concurrent.TimeUnit

internal class ThumbnailsAdapter(thumbnails: List<GalleryFragment.Thumbnail> = emptyList(),
                                 private var onItemInteractListener: OnItemInteractListener?)
    : RecyclerView.Adapter<ThumbnailsAdapter.ViewHolder>() {

    private val thumbnails: MutableList<GalleryFragment.Thumbnail> = ArrayList()

    init {
        this.thumbnails.addAll(thumbnails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_gallery_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = thumbnails.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(thumbnails[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val mBinding = DataBindingUtil.bind<LayoutGalleryItemBinding>(itemView)

        init {
            mBinding!!.root.setOnClickListener {
                onItemInteractListener?.onThumbnailSelected(thumbnails[adapterPosition])
            }
        }

        fun bindData(thumbnail: GalleryFragment.Thumbnail) {
            Glide.with(itemView.context)
                    .load(thumbnail.uri)
                    .apply(RequestOptions.encodeQualityOf(75))
                    .apply(RequestOptions.centerInsideTransform())
                    .into(mBinding!!.image)

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

    fun setThumbnails(thumbnails: List<GalleryFragment.Thumbnail>) {
        this.thumbnails.apply {
            clear()
            addAll(thumbnails)
        }
    }

    interface OnItemInteractListener {

        fun onThumbnailSelected(thumbnail: GalleryFragment.Thumbnail)
    }
}