package com.smedialink.shop.util

import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.ViewHolder.string(resId: Int): String =
    itemView.context.string(resId)
