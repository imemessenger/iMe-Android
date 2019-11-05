package com.smedialink.bots.data.mapper

import com.smedialink.bots.data.database.TagDbModel
import com.smedialink.bots.domain.model.SmartTag

class BotTagsMapper {

    fun map(from: TagDbModel?, language:String): SmartTag? =
        from?.let {
            val title = if (language == DEFAULT_LANGUAGE) from.title
            else from.locales[language] ?: from.locales[EN_LANGUAGE]
            ?: from.title
            SmartTag(
                id = it.id,
                title = title,
                hidden = it.hidden == 1
            )
        }

    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        const val EN_LANGUAGE = "en"
    }
}
