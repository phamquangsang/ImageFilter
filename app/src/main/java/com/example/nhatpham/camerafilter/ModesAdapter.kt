package com.example.nhatpham.camerafilter

import android.databinding.DataBindingUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nhatpham.camerafilter.databinding.LayoutModeItemBinding

class ModesAdapter(private val modes: List<String> = ArrayList(),
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

    fun getItem(position: Int) = if(itemCount in (position + 1)..0)  "" else modes[position]

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val mBinding: LayoutModeItemBinding? = DataBindingUtil.bind(itemView)

        init {
            mBinding!!.tvMode.setOnClickListener {
                onItemInteractListener?.onModeSelected(modes[adapterPosition], adapterPosition)
            }
        }

        fun bindData(mode: String) {
            mBinding!!.tvMode.text = mode
        }
    }

    interface OnItemInteractListener {

        fun onModeSelected(mode: String, position: Int)
    }
}