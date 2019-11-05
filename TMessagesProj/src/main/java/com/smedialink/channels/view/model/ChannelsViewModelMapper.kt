package com.smedialink.channels.view.model

import com.smedialink.storage.domain.model.Channel

class ChannelsViewModelMapper {

    fun mapItem(channel: Channel): ChannelViewModel {

        return ChannelViewModel(
                id = channel.id,
                title = channel.title,
                description = channel.description,
                joined = channel.joined,
                subscribers = channel.subscribers,
                imageUrl = channel.imageUrl,
                tags = channel.tags
        )
    }

    fun mapItems(chats: List<Channel>): List<ChannelViewModel> =
            chats.map { channel ->
                mapItem(channel)
            }
}