package com.smedialink.countries.model

import com.smedialink.storage.domain.model.Country

class CountryViewModelMapper {

    fun mapItems(countries: List<Country>, language: String): List<CountryViewModel> =
            countries.map { country ->
                CountryViewModel(
                        id = country.id,
                        name =
                        if (language == DEFAULT_LANGUAGE) country.name
                        else country.locales[language] ?: country.locales[EN_LANGUAGE]
                        ?: country.name,
                        checked = country.checked
                )
            }

    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        const val EN_LANGUAGE = "en"
    }

}