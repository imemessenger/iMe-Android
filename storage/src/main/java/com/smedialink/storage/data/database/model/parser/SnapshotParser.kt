package com.smedialink.storage.data.database.model.parser

import com.google.firebase.firestore.QuerySnapshot
import com.smedialink.storage.data.database.model.ChannelCategoryDbModel
import com.smedialink.storage.data.database.model.CountryDbModel
import com.smedialink.storage.domain.model.Channel
import com.smedialink.storage.domain.model.Country

@Suppress("UNCHECKED_CAST")
class SnapshotParser {

    companion object {
        private const val CATEGORIES_FIELD_TITLE = "title"
        private const val CATEGORIES_FIELD_TAGS = "tags"

        private const val CHANNELS_FIELD_TITLE = "title"
        private const val CHANNELS_FIELD_TAGS = "tags"
        private const val CHANNELS_FIELD_DESCRIPTION = "description"
        private const val CHANNELS_FIELD_IMAGE = "image"
        //todo remove CHANNELS_FIELD_IMAGE_BUG
        private const val CHANNELS_FIELD_IMAGE_BUG = "iamge"
        private const val CHANNELS_FIELD_COUNTRIES = "countries"
        private const val CHANNELS_FIELD_SUBSCRIBERS = "subscribers"
        private const val CHANNELS_FIELD_RATING = "rating"
        private const val CHANNELS_FIELD_REVIEWS = "reviews"

        private const val COUNTRIES_FIELD_NAME = "name"
        private const val COUNTRIES_FIELD_LOCALES = "locales"


    }

    fun parseChannelCategories(querySnapshot: QuerySnapshot): List<ChannelCategoryDbModel> {
        val categories: MutableList<ChannelCategoryDbModel> = mutableListOf()
        querySnapshot.documents.forEach {
            val title: String? = it.getString(CATEGORIES_FIELD_TITLE)
            val tags: List<String>? = it.get(CATEGORIES_FIELD_TAGS)as? List<String>
            val locales: MutableMap<String, String> = mutableMapOf()
            (it.get("locales") as HashMap<String, HashMap<String, String>>).forEach {entry->
                locales[entry.key] = entry.value["title"] ?: ""
            }
            if (title != null &&
                    tags != null) {
                categories.add(ChannelCategoryDbModel(
                        id = it.id,
                        title = title,
                        tags = tags.toSet(),
                        locales = locales
                ))
            }
        }
        return categories
    }


    fun parseChannels(querySnapshot: QuerySnapshot): List<Channel> {
        val channels: MutableList<Channel> = mutableListOf()
        querySnapshot.documents.forEach {
            val description: String? = it.getString(CHANNELS_FIELD_DESCRIPTION)
            val title: String? = it.getString(CHANNELS_FIELD_TITLE)
            val subscribers: Long? =
                    when {
                        it.get(CHANNELS_FIELD_SUBSCRIBERS) is String -> it.getString(CHANNELS_FIELD_SUBSCRIBERS)?.toLong()
                        else -> it.getLong(CHANNELS_FIELD_SUBSCRIBERS)
                    }
            val imageUrl: String? =
                    it.getString(CHANNELS_FIELD_IMAGE) ?: it.getString(CHANNELS_FIELD_IMAGE_BUG)
            val tags: List<String>? = it.get(CHANNELS_FIELD_TAGS)as? List<String>
            val countries: List<String>? = it.get(CHANNELS_FIELD_COUNTRIES)as? List<String>

            if (description != null &&
                    subscribers != null &&
                    imageUrl != null &&
                    tags != null &&
                    countries != null &&
                    title != null
            ) {
                channels.add(Channel(
                        id = it.id,
                        title = title,
                        subscribers = subscribers.toInt(),
                        imageUrl = imageUrl,
                        description = description,
                        reviews = it.getLong(CHANNELS_FIELD_REVIEWS)?.toInt() ?: 0,
                        rating = it.getDouble(CHANNELS_FIELD_RATING)?.toFloat() ?: 0f,
                        countries = countries.toSet(),
                        tags = tags.toSet()
                ))
            }
        }
        return channels
    }

    fun parseCountries(querySnapshot: QuerySnapshot): List<Country> {
        val countries: MutableList<Country> = mutableListOf()
        querySnapshot.documents.forEach {
            val name: String? = it.getString(COUNTRIES_FIELD_NAME)
            val locales: Map<String, String>? = it.get(COUNTRIES_FIELD_LOCALES)as? Map<String, String>

            if (name != null &&
                    locales != null) {
                countries.add(Country(
                        id = it.id,
                        name = name,
                        locales = locales
                ))
            }
        }
        return countries
    }

    fun parseCountriesToDb(querySnapshot: QuerySnapshot): List<CountryDbModel> {
        val countries: MutableList<CountryDbModel> = mutableListOf()
        querySnapshot.documents.forEach {
            val name: String? = it.getString(COUNTRIES_FIELD_NAME)
            val locales: Map<String, String>? = it.get(COUNTRIES_FIELD_LOCALES)as? Map<String, String>

            if (name != null &&
                    locales != null) {
                countries.add(CountryDbModel(
                        id = it.id,
                        name = name,
                        locales = locales
                ))
            }
        }
        return countries
    }


}