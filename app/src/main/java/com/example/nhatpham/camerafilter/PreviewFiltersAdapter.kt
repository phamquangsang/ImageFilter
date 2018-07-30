package com.example.nhatpham.camerafilter

import android.content.Context
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import com.example.nhatpham.camerafilter.databinding.LayoutPreviewItemBinding
import com.example.nhatpham.camerafilter.models.Config
import com.example.nhatpham.camerafilter.utils.clickWithDebounce
import com.example.nhatpham.camerafilter.utils.convertDpToPixel

import org.wysaid.nativePort.CGENativeLibrary
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

internal class PreviewFiltersAdapter(context: Context,
                                     private val configs: List<Config> = emptyList(),
                                     private val onItemInteractListener: PreviewFiltersAdapter.OnItemInteractListener?)
    : RecyclerView.Adapter<PreviewFiltersAdapter.ViewHolder>() {

    private val previewSize = convertDpToPixel(context, 40F)
    private val selectedColor = Color.parseColor("#FF7A79")
    private val defaultScale = 1F
    private val selectedScale = 1.05F
    private val threadPool = Executors.newFixedThreadPool(2)

    var imageUri: String? = null
    private var lastSelectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewFiltersAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_preview_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewFiltersAdapter.ViewHolder, position: Int) {
        holder.bindData(configs[position])
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            holder.bindData(configs[position], payloads[0] as Bundle)
        } else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.itemView.context).clear(holder.mBinding!!.imgFilter)
    }

    override fun getItemCount() = configs.size

    fun setNewConfig(newConfig: Config) {
        if (newConfig != configs[lastSelectedPosition]) {
            val newPos = findConfigPos(newConfig)
            if (newPos != null) {
                lastSelectedPosition = newPos
                notifyItemChanged(newPos, bundleOf("selected" to true))
            }
        }
    }

    fun findConfigPos(config: Config): Int? {
        val pos = configs.indexOf(config)
        return if (pos != -1) pos else null
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val mBinding: LayoutPreviewItemBinding? = DataBindingUtil.bind(itemView)

        init {
            itemView.clickWithDebounce {
                val bundle = bundleOf("selected" to true)
                notifyItemChanged(lastSelectedPosition, bundle)
                lastSelectedPosition = adapterPosition
                notifyItemChanged(adapterPosition, bundle)
                onItemInteractListener?.onConfigSelected(configs[adapterPosition])
            }
        }

        fun bindData(config: Config) {
            if (!imageUri.isNullOrEmpty()) {
                Glide.with(itemView.context)
                        .asBitmap()
                        .load(imageUri)
                        .apply(RequestOptions.encodeQualityOf(75))
                        .apply(RequestOptions.overrideOf(previewSize))
                        .apply(RequestOptions.bitmapTransform(object : BitmapTransformation() {
                            override fun updateDiskCacheKey(messageDigest: MessageDigest) {
                                messageDigest.update(config.name.toByteArray())
                            }

                            override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap? {
                                var bitmap: Bitmap? = null
                                val countDownLatch = CountDownLatch(1)
                                threadPool.submit {
                                    bitmap = CGENativeLibrary.filterImage_MultipleEffects(toTransform, config.value, 1.0f)
                                    countDownLatch.countDown()
                                }
                                countDownLatch.await()
                                return bitmap
                            }

                        }))
                        .into(mBinding!!.imgFilter)
            } else {
                Glide.with(itemView.context)
                        .asBitmap()
                        .load(R.drawable.default_filter)
                        .apply(RequestOptions.encodeQualityOf(75))
                        .apply(RequestOptions.overrideOf(previewSize))
                        .apply(RequestOptions.bitmapTransform(object : BitmapTransformation() {
                            override fun updateDiskCacheKey(messageDigest: MessageDigest) {
                                messageDigest.update(config.name.toByteArray())
                            }

                            override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap? {
                                return generateFilteredBitmap(toTransform, config.value)
                            }
                        }))
                        .into(mBinding!!.imgFilter)
            }
            if (lastSelectedPosition == adapterPosition) {
                mBinding.imgFilter.scaleX = selectedScale
                mBinding.imgFilter.scaleY = selectedScale
            } else {
                mBinding.imgFilter.scaleX = defaultScale
                mBinding.imgFilter.scaleY = defaultScale
            }
            mBinding.imgFilter.borderColor = if (lastSelectedPosition == adapterPosition) selectedColor else Color.WHITE
        }

        fun bindData(config: Config, payload: Bundle) {
            if (payload.getBoolean("selected")) {
                val amount = if (lastSelectedPosition == adapterPosition) selectedScale else defaultScale
                mBinding!!.imgFilter.post {
                    mBinding.imgFilter.animate()
                            .scaleX(amount)
                            .scaleY(amount)
                            .setDuration(100)
                            .start()
                }
                mBinding.imgFilter.borderColor = if (lastSelectedPosition == adapterPosition) selectedColor else Color.WHITE
            }
        }

        fun generateFilteredBitmap(toTransform: Bitmap, config: String) : Bitmap? {
            var bitmap: Bitmap? = null
            val countDownLatch = CountDownLatch(1)
            threadPool.submit {
                bitmap = CGENativeLibrary.filterImage_MultipleEffects(toTransform, config, 1.0f)
                countDownLatch.countDown()
            }
            countDownLatch.await()
            return bitmap
        }
    }

    interface OnItemInteractListener {

        fun onConfigSelected(selectedConfig: Config)
    }
}
