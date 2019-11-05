package com.smedialink.channels.view.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.custom.ChannelCategoryTitleView
import com.smedialink.channels.view.custom.ChannelsListView
import com.smedialink.channels.view.model.ChannelCategoryViewModel
import com.smedialink.channels.view.model.ChannelsViewModel
import com.smedialink.shop.view.model.MainPageContent
import org.telegram.ui.Components.RecyclerListView

class ChannelsCategoriesAdapter(private val currentAccount: Int) : RecyclerListView.SelectionAdapter() {

    private var content: MutableList<MainPageContent> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            RecyclerListView.Holder(when (ViewType.fromValue(viewType)) {
                ViewType.CHANNELS_CATEGORY -> ChannelCategoryTitleView(parent.context)
                ViewType.CHANNELS -> ChannelsListView(parent.context, currentAccount)
            })

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (ViewType.fromValue(getItemViewType(position))) {
            ViewType.CHANNELS_CATEGORY -> {
                val contentItem = content[position] as ChannelCategoryViewModel
                (holder.itemView as? ChannelCategoryTitleView)?.setText(contentItem.title)
            }
            ViewType.CHANNELS -> {
                val contentItem = content[position] as ChannelsViewModel
                (holder.itemView as? ChannelsListView)?.setContent(contentItem.items)
            }
        }
    }

    override fun getItemCount(): Int = content.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int =
            if (content[position] is ChannelCategoryViewModel) ViewType.CHANNELS_CATEGORY.value else ViewType.CHANNELS.value

    override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean =
            false

    fun setContent(items: List<MainPageContent>) {
        content.clear()
        content.addAll(items)
        notifyDataSetChanged()
    }

    enum class ViewType(val value: Int) {
        CHANNELS_CATEGORY(0),
        CHANNELS(1);

        companion object {
            fun fromValue(value: Int) = values().find { it.value == value } ?: CHANNELS
        }
    }
}
