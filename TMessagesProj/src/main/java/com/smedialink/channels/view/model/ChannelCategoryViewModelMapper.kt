package com.smedialink.channels.view.model

import com.smedialink.storage.domain.model.ChannelCategory

class ChannelCategoryViewModelMapper {

    fun mapToViewModel(channelCategories: List<ChannelCategory>,
                       language: String): List<ChannelCategoryViewModel> =
            channelCategories.map { category ->
                mapToViewModel(category, language)
            }

    fun mapToViewModel(category: ChannelCategory,
                       language: String): ChannelCategoryViewModel =
            ChannelCategoryViewModel(
                    id = category.id,
                    title = if (language == DEFAULT_LANGUAGE) category.title
                    else category.locales[language] ?: category.locales[EN_LANGUAGE]
                    ?: category.title)


    companion object {
        const val DEFAULT_LANGUAGE = "ru"
        const val EN_LANGUAGE = "en"
    }
}