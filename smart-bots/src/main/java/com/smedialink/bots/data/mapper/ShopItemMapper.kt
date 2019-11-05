package com.smedialink.bots.data.mapper

import com.smedialink.bots.data.database.BotsDbModel
import com.smedialink.bots.data.database.TagDbModel
import com.smedialink.bots.domain.model.ShopItem
import java.text.SimpleDateFormat
import java.util.*

class ShopItemMapper{

    private val tagsMapper = BotTagsMapper()

    fun mapList(from: List<BotsDbModel>, language: String, list:List<TagDbModel>): List<ShopItem> =
            from.map { mapItem(it, list, language) }

    fun mapItem(from: BotsDbModel,
                list:List<TagDbModel>,
                language: String): ShopItem {

        val mappedTags = from.tags
                .mapNotNull { tagId -> tagsMapper.map(list.findLast { it.id==tagId },language) }
                .map { tag -> tag.copy(title = tag.title.capitalize()) }
        val title = if (language == DEFAULT_LANGUAGE) from.title
        else from.titleLocales[language] ?: from.titleLocales[EN_LANGUAGE]
        ?: from.title
        val description = if (language == DEFAULT_LANGUAGE) from.description
        else from.descriptionLocales[language] ?: from.descriptionLocales[EN_LANGUAGE]
        ?: from.description

        return ShopItem(
                botId = from.id,
                sku = from.sku,
                avatar = from.avatarRounded,
                title = title,
                language = from.lang,
                description = description,
                installs = from.installs,
                priority = from.priority,
                reviews = from.reviews,
                rating = from.rating,
                ownRating = from.ownRating,
                phrases = from.phrases,
                themes = from.themes,
                created = getDateString(from.created),
                updated = getDateString(from.updated),
                price = from.price,
                downloadLink = from.file,
                tags = mappedTags,
                type = from.type,
                status = from.status
        )
    }

    private fun getDateString(date: Date): String =
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)

    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        const val EN_LANGUAGE = "en"
    }
}
