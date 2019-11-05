package com.smedialink.shop.util

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

fun Context.color(@ColorRes colorId: Int) =
        ContextCompat.getColor(this, colorId)

fun Context.string(@StringRes resId: Int): String =
        resources.getString(resId)

fun Context.pxToDp(px: Int): Int =
        Math.round(px / (Resources.getSystem().displayMetrics.xdpi /
                DisplayMetrics.DENSITY_DEFAULT))
