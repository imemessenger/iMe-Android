package com.smedialink.bots

object BotConstants {
    // Список ботов, встроенных в приложение по умолчанию
    const val ASSISTANT_ID = "assistant"
    const val ASSISTANT_ENG_ID = "assistant_eng"
    const val CONFUCIUS_ID = "confucius"
    const val CONFUCIUS_ENG_ID = "confucius_eng"
    const val DEADPOOL_ID = "deadpool"
    const val DEADPOOL_ENG_ID = "deadpool_eng"
    const val HOLIDAYS_ID = "holidays"
    const val MEMES_ID = "memes"
    const val MEMES_ENG_ID = "memes_eng"
    const val MONROE_ID = "monroe"
    const val MONROE_ENG_ID = "monroe_eng"
    const val SOVIET_FILMS_ID = "soviet_films"
    const val YODA_ID = "yoda"
    const val YODA_ENG_ID = "yoda_eng"
    const val FREQUENT_ANSWERS_ID = "recent"

    // Служебные теги
    const val NEW_TAG = "new"
    const val POPULAR_TAG = "popular"

    val predefinedBots = listOf(
            ASSISTANT_ID,
            ASSISTANT_ENG_ID,
            CONFUCIUS_ENG_ID,
            DEADPOOL_ENG_ID,
            MEMES_ENG_ID,
            MONROE_ENG_ID,
            YODA_ENG_ID,
            CONFUCIUS_ID,
            DEADPOOL_ID,
            HOLIDAYS_ID,
            MEMES_ID,
            MONROE_ID,
            SOVIET_FILMS_ID,
            YODA_ID
    )

    // TODO AIGRAM либо версионирование, либо прописывать хеши изначально встроенных ботов
    val hashes = mapOf(
            ASSISTANT_ID to "KrEBEAPGadlvWF8morbTTQ==",
            CONFUCIUS_ID to "tbDmHFE+1JoAALNxU8WF2A==",
            DEADPOOL_ID to "Y0Jedb8wy3DAOzZUehpAGg==",
            MEMES_ID to "vI6k4K6gbRLDMyZ0jmpGaw==",
            MONROE_ID to "povTTg/CX7acGGhbZnDxpA==",
            SOVIET_FILMS_ID to "JbpfpVwwVNLi48eHFxrPiA==",
            YODA_ID to "iuPmP2uFKGuGG2RbWn0c5Q==",
            ASSISTANT_ENG_ID to "IOMbdDggW8S3hqQtm7xyPg==",
            CONFUCIUS_ENG_ID to "HfcSozit8EwXJ1KbI/LlUw==",
            DEADPOOL_ENG_ID to "NAT4KSWzG4QdMK+yahi0fw==",
            MEMES_ENG_ID to "Qi8VqlNCjslKytBihEhjfw==",
            MONROE_ENG_ID to "8oCwGpkeJWtHdhYqJfz6xA==",
            HOLIDAYS_ID to "U1lmtBOVs9cLAyTMzDkQSA==",
            YODA_ENG_ID to "HR3SNk7bhVWqjTf8PsKtFQ=="
    )
}
