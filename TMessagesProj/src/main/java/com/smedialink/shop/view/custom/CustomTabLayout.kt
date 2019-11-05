package com.smedialink.shop.view.custom

import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.android.material.tabs.TabLayout
import java.lang.reflect.Field

// TabLayout с кастомной минимальной шириной вкладок:
// https://medium.com/@elsenovraditya/set-tab-minimum-width-of-scrollable-tablayout-programmatically-8146d6101efe
// По дизайну изначально видны первые три вкладки, так что DIVIDER_FACTOR оставляем 3
// TODO AIGRAM возможно нужно будет другое значение для планшетов, если будет их поддержка
class CustomTabLayout(context: Context) : TabLayout(context) {

    init {
        initTabMinWidth()
    }

    private fun initTabMinWidth() {
        val wh = getScreenSize(context)
        val tabMinWidth = wh[WIDTH_INDEX] / DIVIDER_FACTOR

        val field: Field

        try {
            field = TabLayout::class.java.getDeclaredField(SCROLLABLE_TAB_MIN_WIDTH)
            field.isAccessible = true
            field.set(this, tabMinWidth)
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private fun isScreenSizeRetrieved(widthHeight: IntArray): Boolean {
        return widthHeight[WIDTH_INDEX] != 0 && widthHeight[HEIGHT_INDEX] != 0
    }

    private fun getScreenSize(context: Context): IntArray {
        val widthHeight = IntArray(2)
        widthHeight[WIDTH_INDEX] = 0
        widthHeight[HEIGHT_INDEX] = 0

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay

        val size = Point()
        display.getSize(size)
        widthHeight[WIDTH_INDEX] = size.x
        widthHeight[HEIGHT_INDEX] = size.y

        if (!isScreenSizeRetrieved(widthHeight)) {
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            widthHeight[0] = metrics.widthPixels
            widthHeight[1] = metrics.heightPixels
        }

        // Last defense. Use deprecated API that was introduced in lower than API 13
        if (!isScreenSizeRetrieved(widthHeight)) {
            widthHeight[0] = display.width // deprecated
            widthHeight[1] = display.height // deprecated
        }

        return widthHeight
    }

    companion object {
        private const val WIDTH_INDEX = 0
        private const val HEIGHT_INDEX = 1
        private const val DIVIDER_FACTOR = 4
        private const val SCROLLABLE_TAB_MIN_WIDTH = "scrollableTabMinWidth"
    }
}
