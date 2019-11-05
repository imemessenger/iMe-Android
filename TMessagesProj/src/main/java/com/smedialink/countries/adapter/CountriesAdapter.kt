package com.smedialink.countries.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smedialink.countries.model.CountryViewModel
import org.telegram.messenger.R

class CountriesAdapter(private val onCountryClickListener: OnCountryClickListener) : RecyclerView.Adapter<CountryViewHolder>() {

    private var items: List<CountryViewModel> = listOf()
    private var filteredItems: List<CountryViewModel> = listOf()
    private var currentFilter: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CountryViewHolder =
            CountryViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.country_item_list, parent, false), onCountryClickListener)

    override fun onBindViewHolder(holder: CountryViewHolder, position: Int) {
        holder.bind(filteredItems[position])
    }

    override fun getItemCount(): Int = filteredItems.size

    fun setCountries(items: List<CountryViewModel>) {
        this.items = items
        if (currentFilter.isBlank()) {
            filteredItems = items
            notifyDataSetChanged()
        } else {
            filter(currentFilter)
        }
    }

    fun filter(filter: String) {
        currentFilter = filter
        filteredItems = items.filter { it.name.contains(currentFilter, ignoreCase = true) }
        notifyDataSetChanged()
    }

    interface OnCountryClickListener {
        fun onCountryClick(country: CountryViewModel)
    }
}