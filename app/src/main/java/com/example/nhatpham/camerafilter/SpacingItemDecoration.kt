package com.example.nhatpham.camerafilter

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View


internal class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        outRect.right = space
        outRect.bottom = space
        outRect.top = space

        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space
        } else {
            outRect.top = 0
        }
    }
}