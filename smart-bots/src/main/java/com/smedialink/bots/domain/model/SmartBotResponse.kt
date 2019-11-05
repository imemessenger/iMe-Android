package com.smedialink.bots.domain.model

data class SmartBotResponse(
    val id: String,
    val name: String,
    val localAvatar: Int,
    val remoteAvatar: String,
    val tag: String,
    val gif: String,
    val link: String,
    val answers: List<String>
)
