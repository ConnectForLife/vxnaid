package com.jnj.vaccinetracker.common.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.NestedScrollView

/**
 * A custom NestedScrollView that prevents auto-scrolling when child views request focus.
 * This solves the issue where EditText fields in RecyclerViews/radio buttons cause unwanted scrolling.
 * Allows touch-initiated focus (user taps) but blocks programmatic/layout-driven focus to prevent glitching.
 */
class NonScrollingNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {

    private var isTouchFocusing = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            isTouchFocusing = true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun requestChildFocus(child: View?, focused: View?) {
        if (isTouchFocusing) {
            isTouchFocusing = false
            super.requestChildFocus(child, focused)
        }
    }

    override fun requestChildRectangleOnScreen(
        child: View,
        rectangle: Rect,
        immediate: Boolean
    ): Boolean {
        return false
    }
}
