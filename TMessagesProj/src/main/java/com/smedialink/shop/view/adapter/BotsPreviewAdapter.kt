package com.smedialink.shop.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.bots.BotConstants
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.domain.model.ShopItem
import com.smedialink.smartpanel.extension.loadFrom
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.shop_item_grid.*
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.ui.Components.RecyclerListView

class BotsPreviewAdapter(private val currentAccount: Int) : RecyclerListView.SelectionAdapter() {

    private var content: MutableList<ShopItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.shop_item_grid, parent, false)
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

                // Скрываем все при инициализации
                bot_installed_icon.visibility = View.GONE
                bot_new_label.visibility = View.GONE

                // Отображаем новинку
                if (item.tags.map { it.id }.contains(BotConstants.NEW_TAG)) {
                    bot_new_label.visibility = View.VISIBLE
                } else {
                    bot_new_label.visibility = View.GONE
                }

                // Отображаем цену или статус Free
                if (item.price.isNullOrBlank()) {
                    bot_status.text = "Free"
                }else{
                    bot_status.text = item.price
                }
                // Метка о том что бот установлен (если да то скрываем остальное)
                if (item.status != BotStatus.PAID) {
                    bot_installed_icon.visibility = View.VISIBLE
                    bot_new_label.visibility = View.GONE
                }
            }

            bot_avatar.setOnClickListener {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botItemClicked, item)
            }

            bot_name.setOnClickListener {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.botItemClicked, item)
            }
        }
    }
}
