package com.smedialink.bots.data.model

import com.smedialink.bots.BotConstants
import com.smedialink.bots.R

enum class BotType(val resId: Int) {
    NEURO(R.string.bot_label_neuro),
    RECENT(R.string.bot_label_normal),
    HOLIDAYS(R.string.bot_label_normal);

    companion object {
        fun resolveByName(name: String): BotType {
            return values().find { it.name == name } ?: NEURO
        }

        fun resolveById(botId: String): BotType =
            when (botId) {
                BotConstants.HOLIDAYS_ID -> HOLIDAYS
                BotConstants.FREQUENT_ANSWERS_ID -> RECENT
                else -> NEURO
            }
    }
}
