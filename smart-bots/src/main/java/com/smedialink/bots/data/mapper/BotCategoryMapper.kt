package com.smedialink.bots.data.mapper

import com.smedialink.bots.data.database.BotsCategoryDbModel
import com.smedialink.bots.domain.model.SmartBotCategory

class BotCategoryMapper {

    fun mapList(from: List<BotsCategoryDbModel>, language: String): List<SmartBotCategory> =
            from.mapNotNull { mapItem(it, language) }

    private fun mapItem(from: BotsCategoryDbModel?, language: String): SmartBotCategory? =
            from?.let {
                SmartBotCategory(
                        id = it.id,
                        title =  if (language == DEFAULT_LANGUAGE) from.title
                        else from.locales[language] ?: from.locales[EN_LANGUAGE]
                        ?: from.title,
                        priority = from.priority,
                        tags = from.tags
                )
            }

    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        const val EN_LANGUAGE = "en"
    }
}
