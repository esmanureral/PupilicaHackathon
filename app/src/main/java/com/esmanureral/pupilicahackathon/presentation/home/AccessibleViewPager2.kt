package com.esmanureral.pupilicahackathon.presentation.home

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2

class AccessibleViewPager2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val viewPager2: ViewPager2 = ViewPager2(context, attrs).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
    }
    private var onTouchCallback: (() -> Unit)? = null

    init {
        addView(viewPager2)
    }

    fun setOnTouchCallback(callback: () -> Unit) {
        onTouchCallback = callback
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = super.onTouchEvent(event)
        
        if (event.action == MotionEvent.ACTION_DOWN) {
            onTouchCallback?.invoke()
        }
        
        return result
    }

    override fun performClick(): Boolean {
        onTouchCallback?.invoke()
        return super.performClick()
    }

    var adapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>?
        get() = viewPager2.adapter
        set(value) {
            viewPager2.adapter = value
        }
    
    var currentItem: Int
        get() = viewPager2.currentItem
        set(value) = viewPager2.setCurrentItem(value, false)
    

    fun registerOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        viewPager2.registerOnPageChangeCallback(callback)
    }
}
