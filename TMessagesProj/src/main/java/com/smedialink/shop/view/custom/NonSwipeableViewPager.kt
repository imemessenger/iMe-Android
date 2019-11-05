package com.smedialink.shop.view.custom

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager


class NonSwipeableViewPager(context: Context, attrs: AttributeSet?=null) : ViewPager(context, attrs) {

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return false
    }


}