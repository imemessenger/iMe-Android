package com.smedialink.bots.analytics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
class AigramAnalytics private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AigramAnalytics? = null

        fun getInstance(context: Context): AigramAnalytics {
            if (INSTANCE == null) {
                synchronized(AigramAnalytics::class) {
                    INSTANCE = AigramAnalytics(context)
                }
            }
            return INSTANCE!!
        }

        private const val USER_ID_KEY = "user_id"
        private const val DATE_KEY = "date"
        private const val VALUE_KEY = "value"
        private const val TAB_NAME_KEY = "tab_name"
        private const val BOT_NAME_KEY = "bot_name"
        private const val BOTS_LIST_KEY = "bots_list"
        private const val INTENT_NAME_KEY = "intent_name"

        private const val AUTO_BOTS_STATE_EVENT = "autobots_switch_state"
        private const val BOTS_BUTTON_PRESSED_EVENT = "bots_button_pressed"
        private const val CLOSE_BOTS_OPEN_KEYBOARD_EVENT = "transitions_to_keyboard"
        private const val RECENT_TAB_EVENT = "recent_tab_appeared"
        private const val MESSAGE_COUNT_EVENT = "messages_count_all"
        private const val BOT_KNOWN_MESSAGE_EVENT = "messages_count_detected"
        private const val FOLDERS_COUNT_EVENT = "folders_count"
        private const val DIALOGS_TAB_OPENING_EVENT = "dialog_tab_opened"
        private const val BOTS_PANEL_TAB_OPENING_EVENT = "bot_tab_opened"
        private const val BOT_GENERAL_EVENT = "bots_triggered"
        private const val BOT_ANSWER_CHOSEN_EVENT = "bot_answer_chosen"
        private const val BOT_DISABLED_EVENT = "bot_disabled"
    }

    private val dateFormatter: SimpleDateFormat by lazy {
        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
    }

    private val dateTimeLabel: String
        get() = dateFormatter.format(Date())

    private val analytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    // DONE(1) Состояние переключателя "Автоматические боты"
    fun logAutoBotsSwitchState(userId: Long, state: Boolean) {
        val value = if (state) 1L else 0L
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
            putLong(VALUE_KEY, value)
            putString(DATE_KEY, dateTimeLabel)
        }

        analytics.logEvent(AUTO_BOTS_STATE_EVENT, params)
    }

    // DONE(2) У пользователя отключен показ ботов и была нажата кнопка чтобы посмотреть ответы ботов
    fun logBotsButtonPressed(userId: Long) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
        }
        analytics.logEvent(BOTS_BUTTON_PRESSED_EVENT, params)
    }

    // DONE(3) У пользователя включен показ ботов, но вместо выбора ответов сразу нажата кнопка клавиатуры
    fun logBotsClosedForKeyboardEvent(userId: Long) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
        }
        analytics.logEvent(CLOSE_BOTS_OPEN_KEYBOARD_EVENT, params)
    }

    // DONE(4) Общее количество сообщений в чате
    fun logTotalMessageCount(userId: Long, count: Int) {
        if (count > 1) {
            val params = Bundle().apply {
                putLong(USER_ID_KEY, userId)
                putLong(VALUE_KEY, count.toLong())
            }
            analytics.logEvent(MESSAGE_COUNT_EVENT, params)
        }
    }

    // DONE(5) Количество сообщений в чате, на которые сработал хотя бы один бот
    fun logDetectedMessageCount(userId: Long, count: Int) {
        if (count > 1) {
            val params = Bundle().apply {
                putLong(USER_ID_KEY, userId)
                putLong(VALUE_KEY, count.toLong())
            }
            analytics.logEvent(BOT_KNOWN_MESSAGE_EVENT, params)
        }
    }

    // DONE(6) Пользователь выбрал какой-то ответ у бота
    fun logAnswerChosenEvent(userId: Long, bot: String, intent: String) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
            putString(BOT_NAME_KEY, bot)
            putString(INTENT_NAME_KEY, intent)
        }
        analytics.logEvent(BOT_ANSWER_CHOSEN_EVENT, params)
    }

    // DONE(7) Просмотр вкладки панели ботов (если ботов больше трех)
    fun logPanelTabOpeningEvent(userId: Long, bot: String) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
            putString(BOT_NAME_KEY, bot)
        }
        analytics.logEvent(BOTS_PANEL_TAB_OPENING_EVENT, params)
    }

    // DONE(8) Появилась вкладка "Часто используемые"
    fun logRecentsTriggeredEvent(userId: Long) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
        }
        analytics.logEvent(RECENT_TAB_EVENT, params)
    }

    // DONE(9) Пользователь зашел в магазин и отключил бота
    fun logBotDisabledEvent(userId: Long, bot: String) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
            putString(DATE_KEY, dateTimeLabel)
            putString(BOT_NAME_KEY, bot)
        }
        analytics.logEvent(BOT_DISABLED_EVENT, params)
    }

    // DONE(10) Количество созданных пользователем папок
    fun logUserFoldersCount(userId: Long, count: Int) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
            putLong(VALUE_KEY, count.toLong())
            putString(DATE_KEY, dateTimeLabel)
        }
        analytics.logEvent(FOLDERS_COUNT_EVENT, params)
    }

    // DONE(11) Просмотр вкладки чатов
    fun logDialogTabOpeningEvent(userId: Long, tab: String) {
        val params = Bundle().apply {
            putLong(USER_ID_KEY, userId)
            putString(TAB_NAME_KEY, tab)
        }
        analytics.logEvent(DIALOGS_TAB_OPENING_EVENT, params)
    }

    // DONE() Срабатывание ботов
    fun logTriggeredBots(userId: Long, botsListString: String) {
        if (botsListString.isNotEmpty()) {
            val params = Bundle().apply {
                putLong(USER_ID_KEY, userId)
                putString(BOTS_LIST_KEY, botsListString)
            }
            analytics.logEvent(BOT_GENERAL_EVENT, params)
        }
    }
}
