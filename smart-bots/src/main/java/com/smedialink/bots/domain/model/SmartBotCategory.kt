package com.smedialink.bots.domain.model

data class SmartBotCategory(
    val id: String,
    val title: String,
    val priority: Int,
    val tags: List<String>
)
