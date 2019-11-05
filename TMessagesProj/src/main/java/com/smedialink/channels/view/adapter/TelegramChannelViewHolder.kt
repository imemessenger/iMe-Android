package com.smedialink.channels.view.adapter

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.model.ChannelViewModel
import com.smedialink.shop.view.custom.SmartButton
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.telegram_channel_item_list.*
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R

class TelegramChannelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

    override val containerView: View = itemView
    // Здесь используются телеграмовские классы  загрузки картинки так как через Glide загрузить не получится (слишком дико хранятся картинки в телеговской сущности диалога)
    // TelegramImageView -> моя обертка над телеграмовской вьюхой картинок
    @SuppressLint("SetTextI18n")
    fun bindTo(item: ChannelViewModel, currentAccount: Int) {
        with(item) {
            item.dialog?.id?.let { id->
                MessagesController.getInstance(currentAccount).getChat(-id.toInt())?.let {chat->
                    channel_avatar.setInfo(chat)
                }
            }
            channel_name.text = title

            join_button.buttonState = SmartButton.resolveState(joined)
            join_button.buttonText = if (joined) LocaleController.getString("AiLeaveChannel",R.string.AiLeaveChannel)
            else LocaleController.getString("ChannelJoin",R.string.ChannelJoin)

            progressBar.visibility = View.INVISIBLE
            join_button.visibility = View.VISIBLE
        }

        val channelClickListener = View.OnClickListener { NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.channelItemClicked, item) }
        channel_avatar.setOnClickListener(channelClickListener)
        channel_name.setOnClickListener(channelClickListener)
        item_container.setOnClickListener(channelClickListener)

        join_button.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            join_button.visibility = View.INVISIBLE
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.joinChannelButtonClicked, item)
        }
    }
}