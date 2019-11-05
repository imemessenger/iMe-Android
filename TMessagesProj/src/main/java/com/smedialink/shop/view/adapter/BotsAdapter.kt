package com.smedialink.shop.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.domain.model.ShopItem
import com.smedialink.shop.view.custom.SmartButton
import com.smedialink.smartpanel.extension.loadFrom
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.shop_item_list.*
import org.telegram.messenger.LocaleController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.ui.Components.RecyclerListView

class BotsAdapter(private val currentAccount: Int) : RecyclerListView.SelectionAdapter() {

    private var content: MutableList<ShopItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.shop_item_list, parent, false)
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

    fun setContent(newContent: List<ShopItem>) {
        content.clear()
        content.addAll(newContent)
        notifyDataSetChanged()
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {

        override val containerView: View = itemView

        fun bindTo(item: ShopItem, currentAccount: Int) {
            with(item) {
                bot_avatar.loadFrom(avatar, itemView.context)
                bot_name.text = title
                bot_description.text = description

                if (tags.map { it.id }.contains(BotConstants.NEW_TAG)) {
                    bot_new_label.visibility = View.VISIBLE
                } else {
                    bot_new_label.visibility = View.GONE
                }

                shop_button.buttonState = SmartButton.resolveState(status)

                shop_button.buttonText =
                        when (status) {
                            BotStatus.PAID -> item.price ?: "Free"
                            BotStatus.AVAILABLE -> LocaleController.getString("shop_button_label_download",R.string.shop_button_label_download)
                            BotStatus.UPDATE_AVAILABLE ->  LocaleController.getString("shop_button_label_update",R.string.shop_button_label_update)
                            BotStatus.DOWNLOADING ->  LocaleController.getString("shop_button_label_downloading",R.string.shop_button_label_downloading)
                            BotStatus.ENABLED ->  LocaleController.getString("shop_button_label_disable",R.string.shop_button_label_disable)
                            BotStatus.DISABLED ->  LocaleController.getString("shop_button_label_enable",R.string.shop_button_label_enable)
                        }
            }

            bot_avatar.setOnClickListener {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botItemClicked, item)
            }
            bot_name.setOnClickListener {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botItemClicked, item)
            }
            bot_description.setOnClickListener {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botItemClicked, item)
            }
            shop_button.setOnClickListener {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botButtonClicked, item)
            }
        }
    }
}
