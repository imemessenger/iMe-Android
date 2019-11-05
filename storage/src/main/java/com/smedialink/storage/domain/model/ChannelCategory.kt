package com.smedialink.storage.domain.model

data class ChannelCategory(val id: String,
                           val title: String,
                           val tags: Set<String>,
                           val locales: Map<String, String>)