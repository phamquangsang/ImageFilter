package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.support.v4.graphics.drawable.RoundedBitmapDrawable
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
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
import java.util.concurrent.Executors

class PreviewImagesAdapter(private val configs: List<String> = ArrayList(),
                           private val onItemInteractListener: PreviewImagesAdapter.OnItemInteractListener?)
    : RecyclerView.Adapter<PreviewImagesAdapter.ViewHolder>() {

    val appExecutor = Executors.newSingleThreadExecutor()
    val mainHandler = Handler()

    var imageUri: String? = null
    var selectedPos: Int = 0

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
                val bundle = bundleOf("selected" to true)
                notifyItemChanged(selectedPos)
                selectedPos = adapterPosition
                notifyItemChanged(selectedPos)
                onItemInteractListener?.onConfigSelected(configs[selectedPos])
            }
        }

        fun bindData(config: String) {
            if (!imageUri.isNullOrEmpty()) {
                Glide.with(itemView.context)
                        .asBitmap()
                        .load(imageUri)
                        .apply(RequestOptions.centerInsideTransform())
                        .into(object : BitmapImageViewTarget(mBinding!!.imgFilter) {

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                appExecutor.submit {
                                    val result = CGENativeLibrary.filterImage_MultipleEffects(resource, config, 1.0f)
                                    mainHandler.post {
                                        getView().setImageBitmap(result)
                                    }
                                }
                            }
                        })
            } else {
                Glide.with(itemView.context)
                        .asBitmap()
                        .load(R.drawable.default_filter)
                        .apply(RequestOptions.centerInsideTransform())
                        .into(object : BitmapImageViewTarget(mBinding!!.imgFilter) {

                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                appExecutor.submit {
                                    val result = CGENativeLibrary.filterImage_MultipleEffects(resource, config, 1.0f)
                                    mainHandler.post {
                                        getView().setImageBitmap(result)
                                    }
                                }
                            }
                        })
            }
            mBinding.imgFilter.borderWidth = if(selectedPos == adapterPosition) 4F else 0F
        }
    }

    interface OnItemInteractListener {

        fun onConfigSelected(selectedConfig: String)
    }
}
