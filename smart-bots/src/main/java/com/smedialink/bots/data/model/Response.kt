package com.smedialink.bots.data.model

data class Response(
    val botId: String,
    val tag: String,
    val gif: String = "",
    val link: String = "",
    val answers: List<String>
)
