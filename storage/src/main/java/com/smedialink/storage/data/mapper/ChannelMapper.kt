package com.smedialink.storage.data.mapper

import com.smedialink.storage.data.database.model.ChannelDbModel
import com.smedialink.storage.domain.model.Channel

class ChannelMapper {

    fun mapFromDb(channel: ChannelDbModel): Channel = Channel(
            id = channel.id,
            title = channel.title,
            subscribers = channel.subscribers,
            joined = channel.joined,
            imageUrl = channel.imageUrl,
            description = channel.description,
            tags = channel.tags,
            ownRating = channel.ownRating,
            countries = channel.countries,
            reviews = channel.reviews.toInt(),
            rating = channel.rating)

    fun mapToDb(channel: Channel): ChannelDbModel = ChannelDbModel(
            id = channel.id,
            title = channel.title,
            subscribers = channel.subscribers,
            joined = channel.joined,
            imageUrl = channel.imageUrl,
            description = channel.description,
            tags = channel.tags,
            countries = channel.countries,
            ownRating = channel.ownRating,
            reviews = channel.reviews.toLong(),
            rating = channel.rating
    )

    fun mapToDb(channels: List<Channel>): List<ChannelDbModel> =
            channels.map { mapToDb(it) }

    fun mapFromDb(channels: List<ChannelDbModel>): List<Channel> =
            channels.map { mapFromDb(it) }

}