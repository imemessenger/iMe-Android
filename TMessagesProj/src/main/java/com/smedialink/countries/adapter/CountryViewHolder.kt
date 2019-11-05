package com.smedialink.countries.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.countries.model.CountryViewModel
import kotlinx.android.synthetic.main.country_item_list.view.*

class CountryViewHolder(itemView: View,
                        private val click: CountriesAdapter.OnCountryClickListener) : RecyclerView.ViewHolder(itemView) {

    fun bind(country: CountryViewModel) {
        with(itemView) {
            textCountryName.text = country.name
            checkbox.isChecked = country.checked
            rootView.setOnClickListener {
                click.onCountryClick(country)
            }
        }
    }
}