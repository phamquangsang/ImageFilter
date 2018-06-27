package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.nhatpham.camerafilter.databinding.LayoutPreviewItemBinding

import org.wysaid.nativePort.CGENativeLibrary
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch

class PreviewImagesAdapter(private val configs: List<String> = ArrayList(),
                           private val onItemInteractListener: PreviewImagesAdapter.OnItemInteractListener?)
    : RecyclerView.Adapter<PreviewImagesAdapter.ViewHolder>() {

    var imageUri: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewImagesAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_preview_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewImagesAdapter.ViewHolder, position: Int) {
        holder.bindData(configs[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView.context).clear(holder.mBinding!!.imgFilter)
    }

    override fun getItemCount() = configs.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val mBinding: LayoutPreviewItemBinding? = DataBindingUtil.bind(itemView)

        init {
            itemView.setOnClickListener {
                onItemInteractListener?.onConfigSelected(configs[adapterPosition])
            }
        }

        fun bindData(config: String) {
            if(!imageUri.isNullOrEmpty()) {
                Glide.with(itemView.context)
                        .asBitmap()
                        .load(imageUri)
                        .apply(RequestOptions.centerInsideTransform())
                        .apply(RequestOptions.bitmapTransform(object : BitmapTransformation() {

                            override fun updateDiskCacheKey(messageDigest: MessageDigest) {
                                messageDigest.update(config.toByteArray())
                            }

                            override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int) =
                                    CGENativeLibrary.filterImage_MultipleEffects(toTransform, config, 1.0f)

                        }))
                        .into(object : BitmapImageViewTarget(mBinding!!.imgFilter) {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                getView().setImageDrawable(RoundedBitmapDrawableFactory.create(mBinding!!.imgFilter.context.resources, resource)
                                        .apply {
                                            cornerRadius = 4F
                                        })
                            }
                        })
            } else {
                Glide.with(itemView.context)
                        .asBitmap()
                        .load(R.drawable.default_filter)
                        .apply(RequestOptions.centerInsideTransform())
                        .apply(RequestOptions.bitmapTransform(object : BitmapTransformation() {

                            override fun updateDiskCacheKey(messageDigest: MessageDigest) {
                                messageDigest.update(config.toByteArray())
                            }

                            override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int) =
                                    CGENativeLibrary.filterImage_MultipleEffects(toTransform, config, 1.0f)

                        }))
                        .into(object : BitmapImageViewTarget(mBinding!!.imgFilter) {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                getView().setImageDrawable(RoundedBitmapDrawableFactory.create(mBinding!!.imgFilter.context.resources, resource)
                                        .apply {
                                    cornerRadius = 4F
                                })
                            }
                        })
            }
        }
    }

    interface OnItemInteractListener {

        fun onConfigSelected(selectedConfig: String)
    }
}
