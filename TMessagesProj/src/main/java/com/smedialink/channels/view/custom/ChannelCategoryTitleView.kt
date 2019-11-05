package com.smedialink.channels.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import org.telegram.ui.Components.LayoutHelper

@SuppressLint("ViewConstructor")
class ChannelCategoryTitleView(context: Context) : FrameLayout(context) {

    private var title: TextView = TextView(context)

    init {
        title.setTextColor(Color.parseColor("#242424"))
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), Gravity.CENTER, 16f, 0f, 16f, 0f))
    }

    fun setText(text: String) {
        title.text = text
    }
}