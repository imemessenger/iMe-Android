package com.smedialink.languages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.telegram.messenger.R

class LanguagesAdapter(private val onCountryClickListener: OnLanguageClickListener) : RecyclerView.Adapter<LanguageViewHolder>() {

    private var items: List<LanguageViewModel> = listOf()
    private var currentFilter: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder =
            LanguageViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.country_item_list, parent, false), onCountryClickListener)

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setLanguages(items: List<LanguageViewModel>) {
        this.items = items
        notifyDataSetChanged()
    }


    interface OnLanguageClickListener {
        fun onLanguageClick(language: LanguageViewModel)
    }
}