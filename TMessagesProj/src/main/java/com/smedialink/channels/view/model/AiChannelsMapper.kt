package com.smedialink.channels.view.model

import org.telegram.messenger.MessagesController
import org.telegram.tgnet.TLRPC

class AiChannelsMapper {

    fun mapItems(chats: List<TLRPC.Dialog>, joined: Boolean = false, messagesController: MessagesController): List<ChannelViewModel> =
            chats.map { dialog ->
                val chat = messagesController.getChat(-(dialog.id.toInt()))
                ChannelViewModel(
                        id = chat?.username ?: "",
                        title = chat?.title ?: "",
                        description = "",
                        joined = joined,
                        subscribers = chat?.participants_count ?: 0,
                        imageUrl = "",
                        dialog = dialog

                )
            }
}