package com.smedialink.channels.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.model.ChannelViewModel
import org.telegram.messenger.R
import org.telegram.ui.Components.RecyclerListView

class ChannelsAdapter(private val currentAccount: Int) : RecyclerListView.SelectionAdapter() {

    private var content: MutableList<ChannelViewModel> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (ViewType.fromValue(viewType)) {
                ViewType.VIEW_TYPE_AI_CHANNEL -> AiChannelViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.channel_item_list, parent, false))
                ViewType.VIEW_TYPE_TELEGRAM_CHANNEL -> TelegramChannelViewHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.telegram_channel_item_list, parent, false))
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when  {
            holder is AiChannelViewHolder -> holder.bindTo(content[position], currentAccount)
            holder is TelegramChannelViewHolder-> holder.bindTo(content[position], currentAccount)
        }
    }

    override fun getItemCount(): Int = content.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int =
            if (content[position].dialog == null) ViewType.VIEW_TYPE_AI_CHANNEL.value
            else ViewType.VIEW_TYPE_TELEGRAM_CHANNEL.value

    override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean = true

    fun setContent(newContent: List<ChannelViewModel>) {
        content.clear()
        content.addAll(newContent)
        notifyDataSetChanged()
    }



    enum class ViewType(val value: Int) {
        VIEW_TYPE_AI_CHANNEL(0),
        VIEW_TYPE_TELEGRAM_CHANNEL(1);

        companion object {
            fun fromValue(value: Int): ViewType = values().find { it.value == value }
                    ?: VIEW_TYPE_AI_CHANNEL
        }
    }
}
