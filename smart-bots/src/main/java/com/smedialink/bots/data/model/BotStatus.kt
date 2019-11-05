package com.smedialink.bots.data.model

enum class BotStatus {
    AVAILABLE, PAID, UPDATE_AVAILABLE, DOWNLOADING, ENABLED, DISABLED;

    companion object {
        fun resolve(name: String): BotStatus =
            BotStatus.values().firstOrNull { it.name == name } ?: AVAILABLE
    }
}
