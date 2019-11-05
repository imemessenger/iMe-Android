package com.smedialink.channels.view.model

import org.telegram.tgnet.TLRPC

data class ChannelViewModel(val id: String,
                            val title: String,
                            val subscribers: Int,
                            var joined: Boolean = false,
                            val imageUrl: String,
                            val description: String,
                            val rating: Float = 0f,
                            val ownRating: Int=0,
                            val reviews: Int = 0,
                            val dialog: TLRPC.Dialog?=null,
                            val tags: Set<String> = emptySet() )