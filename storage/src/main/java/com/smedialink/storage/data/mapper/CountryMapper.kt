package com.smedialink.storage.data.mapper

import com.smedialink.storage.data.database.model.CountryDbModel
import com.smedialink.storage.domain.model.Country

class CountryMapper {

    fun mapFromDb(model: CountryDbModel, currentCountry: String): Country = Country(
            id = model.id,
            name = model.name,
            checked = currentCountry == model.id,
            locales = model.locales)

    fun mapToDb(entity: Country): CountryDbModel = CountryDbModel(
            id = entity.id,
            name = entity.name,
            locales = entity.locales)


    fun mapToDb(entities: List<Country>): List<CountryDbModel> =
            entities.map { mapToDb(it) }

    fun mapFromDb(models: List<CountryDbModel>, currentCountry: String): List<Country> =
            models.map { mapFromDb(it,currentCountry) }

}