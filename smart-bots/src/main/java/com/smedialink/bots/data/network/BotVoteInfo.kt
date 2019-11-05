package com.smedialink.bots.data.network

import com.google.gson.annotations.SerializedName

data class BotVoteInfo(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("created")
    val created: Long,
    @SerializedName("bot_id")
    val name: String,
    @SerializedName("rating")
    val rating: String
)
