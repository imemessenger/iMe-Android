package com.smedialink.storage.data.mapper

import com.smedialink.storage.data.database.model.ChannelCategoryDbModel
import com.smedialink.storage.domain.model.ChannelCategory

class ChannelCategoryMapper {

    fun mapFromDb(channelCategory: ChannelCategoryDbModel): ChannelCategory = ChannelCategory(
            id = channelCategory.id,
            title = channelCategory.title,
            tags = channelCategory.tags,
            locales = channelCategory.locales)


}