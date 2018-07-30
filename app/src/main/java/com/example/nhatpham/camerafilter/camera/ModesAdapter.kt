package com.example.nhatpham.camerafilter.camera

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nhatpham.camerafilter.R
import com.example.nhatpham.camerafilter.databinding.LayoutModeItemBinding
import com.example.nhatpham.camerafilter.utils.clickWithDebounce

internal class ModesAdapter(private val modes: List<CameraMode> = ArrayList(),
                            private val onItemInteractListener: OnItemInteractListener?)
    : RecyclerView.Adapter<ModesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_mode_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = modes.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(modes[position])
    }

    fun getItem(position: Int) = if(itemCount in (position + 1)..0)  null else modes[position]

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val mBinding: LayoutModeItemBinding? = DataBindingUtil.bind(itemView)

        init {
            mBinding!!.tvMode.clickWithDebounce {
                onItemInteractListener?.onModeSelected(modes[adapterPosition], adapterPosition)
            }
        }

        fun bindData(mode: CameraMode) {
            mBinding!!.tvMode.text = mode.toString()
        }
    }

    interface OnItemInteractListener {

        fun onModeSelected(mode: CameraMode, position: Int)
    }
}