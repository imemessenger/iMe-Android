package com.smedialink.bots.data.network

import com.google.gson.annotations.SerializedName

data class BotRemoteInfo(
    @SerializedName("name")
    val name: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("installation")
    val installs: String,
    @SerializedName("deletion")
    val uninstalls: String,
    @SerializedName("theme")
    val themes: String,
    @SerializedName("phrase")
    val phrases: String,
    @SerializedName("weight")
    val weight: String,
    @SerializedName("rating")
    val rating: String,
    @SerializedName("votings")
    val votings: String
)