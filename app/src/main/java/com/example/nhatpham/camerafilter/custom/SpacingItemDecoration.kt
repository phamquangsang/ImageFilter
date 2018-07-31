package com.example.nhatpham.camerafilter.custom

import android.graphics.Rect
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View


internal class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        val layoutManager = parent.layoutManager
        if(layoutManager is GridLayoutManager) {
            outRect.set(space, space, space, space)
        }
    }
}