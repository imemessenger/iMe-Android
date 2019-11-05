package com.smedialink.channels.view.adapter

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.model.ChannelViewModel
import com.smedialink.smartpanel.extension.loadFrom
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.channel_item_grid.*
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.RecyclerListView

class ChannelsPreviewAdapter(private val currentAccount: Int) : RecyclerListView.SelectionAdapter() {

    private var content: MutableList<ChannelViewModel> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.channel_item_grid, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ItemViewHolder
        holder.bindTo(content[position], currentAccount)
    }

    override fun getItemCount(): Int = content.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean = true

    fun setContent(newContent: List<ChannelViewModel>) {
        content.clear()
        content.addAll(newContent)
        notifyDataSetChanged()
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

        override val containerView: View = itemView

        fun bindTo(item: ChannelViewModel, currentAccount: Int) {
            with(item) {
                channel_avatar.loadFrom(imageUrl, itemView.context, ColorDrawable(ContextCompat.getColor(itemView.context, R.color.colorChannelPlaceholder)))
                channel_name.text = title
                channel_installed_icon.visibility = if (item.joined) View.VISIBLE
                else View.GONE

            }
            channel_name.setTextColor(Theme.getColor(Theme.key_chats_name))

            val onChannelClickListener = View.OnClickListener { NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.channelItemClicked, item) }
            channel_avatar.setOnClickListener(onChannelClickListener)
            channel_name.setOnClickListener(onChannelClickListener)
            channel_avatar.setOnClickListener {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.channelItemClicked, item)
            }
        }
    }
}
