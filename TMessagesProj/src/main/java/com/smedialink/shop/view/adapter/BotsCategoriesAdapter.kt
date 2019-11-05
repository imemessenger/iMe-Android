package com.smedialink.shop.view.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.shop.view.custom.BotCategoryTitleView
import com.smedialink.shop.view.custom.BotsListView
import com.smedialink.shop.view.model.DisplayingBots
import com.smedialink.shop.view.model.DisplayingBotsCategory
import com.smedialink.shop.view.model.MainPageContent
import org.telegram.ui.Components.RecyclerListView

class BotsCategoriesAdapter(private val currentAccount: Int) : RecyclerListView.SelectionAdapter() {

    private var content: MutableList<MainPageContent> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val view = if (viewType == 0) {
            BotCategoryTitleView(parent.context)
        } else {
            BotsListView(parent.context, currentAccount)
        }

        return RecyclerListView.Holder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == 0) {
            val contentItem = content[position] as DisplayingBotsCategory
            (holder.itemView as? BotCategoryTitleView)?.setText(contentItem.title)
        } else {
            val contentItem = content[position] as DisplayingBots
            (holder.itemView as? BotsListView)?.setContent(contentItem.items)
        }
    }

    override fun getItemCount(): Int = content.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getItemViewType(position: Int): Int =
        if (content[position] is DisplayingBotsCategory) 0 else 1

    override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean =
        false

    fun setContent(items: List<MainPageContent>) {
        content.clear()
        content.addAll(items)
        notifyDataSetChanged()
    }
}
