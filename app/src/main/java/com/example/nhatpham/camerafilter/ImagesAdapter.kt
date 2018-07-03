package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.databinding.LayoutGalleryItemBinding

class ImagesAdapter(private val images: List<GalleryFragment.Thumbnail> = emptyList(),
                    private var onItemInteractListener: OnItemInteractListener?)
    : RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_gallery_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = images.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(images[position].uri)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val mBinding = DataBindingUtil.bind<LayoutGalleryItemBinding>(itemView)

        init {
            mBinding!!.root.setOnClickListener {
                onItemInteractListener?.onThumbnailSelected(images[adapterPosition])
            }
        }

        fun bindData(imageUri: String) {
            Glide.with(itemView.context)
                    .load(imageUri)
                    .apply(RequestOptions.centerInsideTransform())
                    .into(mBinding!!.image)
        }
    }

    interface OnItemInteractListener {

        fun onThumbnailSelected(thumbnail: GalleryFragment.Thumbnail)
    }
}