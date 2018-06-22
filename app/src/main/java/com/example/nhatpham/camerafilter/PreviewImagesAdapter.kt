package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.databinding.LayoutPreviewItemBinding

import org.wysaid.nativePort.CGENativeLibrary
import java.security.MessageDigest

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

    override fun getItemCount() = if (imageUri.isNullOrEmpty()) 0 else configs.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val mBinding: LayoutPreviewItemBinding? = DataBindingUtil.bind(itemView)

        init {
            itemView.setOnClickListener {
                onItemInteractListener?.onConfigSelected(configs[adapterPosition])
                Log.i("PreviewImagesAdapter" ,"Configuration selected : "+ configs[adapterPosition])
            }
        }

        fun bindData(config: String) {
            Glide.with(itemView.context)
                    .load(imageUri)
                    .apply(RequestOptions.bitmapTransform(object : BitmapTransformation() {

                        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
                            messageDigest.update(config.toByteArray())
                        }

                        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int) =
                                CGENativeLibrary.filterImage_MultipleEffects(toTransform, config, 1.0f)

                    }))
                    .into(mBinding!!.imgFilter)
        }
    }

    interface OnItemInteractListener {

        fun onConfigSelected(selectedConfig: String)
    }
}
