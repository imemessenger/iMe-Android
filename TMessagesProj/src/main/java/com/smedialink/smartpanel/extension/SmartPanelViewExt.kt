package com.smedialink.smartpanel.extension

import com.smedialink.smartpanel.SmartPanelView
import kotlin.math.roundToInt

fun SmartPanelView.floatToDp(value: Float): Int {
    val density = context.resources.displayMetrics.density

    return when (value) {
        0f -> 0
        else -> Math.ceil((density * value).toDouble()).roundToInt()
    }
}
