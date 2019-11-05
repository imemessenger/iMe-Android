package com.smedialink.channels.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.channels.view.model.ChannelCategoryViewModel
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.category_item_list.*
import org.telegram.messenger.R
import org.telegram.ui.Components.RecyclerListView

class ChannelsCategoriesPreviewAdapter(private val onCategoryClickListener: OnCategoryClickListener) : RecyclerListView.SelectionAdapter() {

    private var content: MutableList<ChannelCategoryViewModel> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):  RecyclerView.ViewHolder =
            CategoryViewHolder(onCategoryClickListener,LayoutInflater.from(parent.context)
                    .inflate(R.layout.category_item_list, parent, false))

    override fun onBindViewHolder(holder:  RecyclerView.ViewHolder, position: Int) {
        (holder as? CategoryViewHolder)?.bindTo(content[position])
    }

    override fun getItemCount(): Int = content.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun isEnabled(holder: RecyclerView.ViewHolder?): Boolean =
            false

    fun setContent(items: List<ChannelCategoryViewModel>) {
        content.clear()
        content.addAll(items)
        notifyDataSetChanged()
    }

    private class CategoryViewHolder(private val onCategoryClickListener: OnCategoryClickListener,itemView: View) : RecyclerView.ViewHolder(itemView), LayoutContainer {
        override val containerView: View = itemView
        fun bindTo(item: ChannelCategoryViewModel) {
            textCategoryTitle.text = item.title
            textCategoryTitle.setOnClickListener {
                onCategoryClickListener.onCategoryClick(item)
            }
        }
    }


    interface OnCategoryClickListener {

        fun onCategoryClick(category: ChannelCategoryViewModel)
    }
}
