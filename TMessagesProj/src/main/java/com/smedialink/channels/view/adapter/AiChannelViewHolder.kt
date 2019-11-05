package com.smedialink.channels.view.adapter

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.model.ChannelViewModel
import com.smedialink.shop.view.custom.SmartButton
import com.smedialink.smartpanel.extension.loadFrom
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.channel_item_list.*
import kotlinx.android.synthetic.main.channel_item_list.view.*
import org.telegram.messenger.LocaleController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.Theme

class AiChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

    override val containerView: View = itemView

    @SuppressLint("SetTextI18n")
    fun bindTo(item: ChannelViewModel, currentAccount: Int) {
        with(item) {
            channel_avatar.loadFrom(imageUrl, itemView.context, ColorDrawable(ContextCompat.getColor(itemView.context, R.color.colorChannelPlaceholder)))
            channel_name.text = title
            channel_name.setTextColor(Theme.getColor(Theme.key_chats_name))
            channel_subscribers.text = LocaleController.formatPluralString("Subscribers", item.subscribers)
            join_button.buttonState = SmartButton.resolveState(joined)
            join_button.buttonText = if (joined) LocaleController.getString("AiLeaveChannel",R.string.AiLeaveChannel)
            else  LocaleController.getString("ChannelJoin",R.string.ChannelJoin)
            progressBar.visibility = View.INVISIBLE
            join_button.visibility = View.VISIBLE

        }
        progressBar.indeterminateDrawable.setColorFilter(Theme.getColor(Theme.key_actionBarDefault),PorterDuff.Mode.MULTIPLY)
        val channelClickListener = View.OnClickListener { NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.channelItemClicked, item) }
        itemView.channel_name.setOnClickListener(channelClickListener)
        itemView.channel_avatar.setOnClickListener(channelClickListener)
        itemView.channel_subscribers.setOnClickListener(channelClickListener)
        itemView.item_container.setOnClickListener(channelClickListener)
        join_button.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            join_button.visibility = View.INVISIBLE
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.joinChannelButtonClicked, item)
        }
    }
}