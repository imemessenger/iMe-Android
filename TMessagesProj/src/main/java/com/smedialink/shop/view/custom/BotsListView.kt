package com.smedialink.shop.view.custom

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.smedialink.bots.domain.model.ShopItem
import com.smedialink.shop.view.adapter.BotsPreviewAdapter
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.R
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RecyclerListView

@SuppressLint("ViewConstructor")
class BotsListView(context: Context, currentAccount: Int) : LinearLayout(context) {

    private val list = RecyclerListView(context)
    private val adapter = BotsPreviewAdapter(currentAccount)
    private val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

    init {
        orientation = VERTICAL
        list.layoutManager = this.layoutManager
        list.adapter = this.adapter
        list.isNestedScrollingEnabled = false
        list.clipToPadding = false
        list.setPadding(AndroidUtilities.dp(6f),0, AndroidUtilities.dp(6f),0)
        addView(list, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, Gravity.CENTER, 0, 12, 0, 0))
        val dividerView = View(context)
        dividerView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorChannelPlaceholder))
        addView(dividerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0f, Gravity.BOTTOM, 16, 13, 8, 12))
    }

    fun setContent(items: List<ShopItem>) {
        adapter.setContent(items)
        list.scrollToPosition(0)
    }
}
