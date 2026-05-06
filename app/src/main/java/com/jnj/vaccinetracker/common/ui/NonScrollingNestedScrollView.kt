package com.jnj.vaccinetracker.common.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.widget.NestedScrollView

/**
 * A custom NestedScrollView that prevents auto-scrolling when child views request focus.
 *
 * **Problem it solves:**
 * When EditText fields or radio buttons receive focus (either via user tap or programmatic),
 * the parent NestedScrollView automatically scrolls to make the focused view visible, causing
 * unwanted visual glitches and focus-stealing behavior during list updates.
 *
 * **Solution:**
 * - Always calls super.requestChildFocus() to maintain proper focus bookkeeping
 * - Overrides requestChildRectangleOnScreen() to block auto-scroll behavior entirely
 *
 * This ensures proper focus state while preventing unwanted scrolling.
 */
class NonScrollingNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {


    override fun requestChildFocus(child: View?, focused: View?) {
        // Always call super to maintain proper ViewGroup focus bookkeeping
        // The requestChildRectangleOnScreen override handles preventing auto-scroll
        super.requestChildFocus(child, focused)
    }

    override fun requestChildRectangleOnScreen(
        child: View,
        rectangle: Rect,
        immediate: Boolean
    ): Boolean {
        // Always prevent scroll-to-focused-child behavior
        // This ensures the view gets focus (for keyboard input) but doesn't scroll into view
        return false
    }
}
