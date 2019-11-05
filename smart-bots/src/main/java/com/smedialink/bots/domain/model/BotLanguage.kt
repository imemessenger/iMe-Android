package com.smedialink.bots.domain.model

enum class BotLanguage(val langCode: String) {
    RU("ru"),
    EN("eng");

    companion object {
        fun fromValue(langCode: String): BotLanguage = values().findLast { it.langCode == langCode }
                ?: RU
    }
}