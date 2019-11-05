package com.smedialink.languages

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.country_item_list.view.*

class LanguageViewHolder(itemView: View,
                         private val click: LanguagesAdapter.OnLanguageClickListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(country: LanguageViewModel) {
        with(itemView) {
            textCountryName.text = country.name
            checkbox.isChecked = country.checked
            rootView.setOnClickListener {
                click.onLanguageClick(country)
            }
        }
    }
}