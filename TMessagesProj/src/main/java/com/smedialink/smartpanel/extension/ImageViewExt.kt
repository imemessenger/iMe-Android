package com.smedialink.smartpanel.extension

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

fun ImageView.loadFrom(url: String, context: Context) {
    if (url.isNotEmpty()) {
        Glide.with(context)
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(this)
    }
}

fun ImageView.loadFrom(url: String, context: Context, placeHolderDrawable:Drawable) {
    if (url.isNotEmpty()) {
        Glide.with(context)
                .load(url)
                .placeholder(placeHolderDrawable)
                .error(placeHolderDrawable)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(this)
    }
}
