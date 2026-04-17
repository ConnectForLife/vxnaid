package com.jnj.vaccinetracker.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

/**
 * A custom NestedScrollView that prevents auto-scrolling when child views request focus.
 * This solves the issue where EditText fields in RecyclerViews cause unwanted scrolling.
 */
class NonScrollingNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var allowScroll = true

    override fun requestChildFocus(child: View?, focused: View?) {
        val previousScrollY = scrollY
        allowScroll = false
        super.requestChildFocus(child, focused)
        post {
            scrollTo(0, previousScrollY)
            allowScroll = true
        }
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (allowScroll) {
            super.onNestedPreScroll(target, dx, dy, consumed, type)
        }
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (allowScroll) {
            super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        }
    }
}
