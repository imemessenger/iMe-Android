package com.smedialink.bots.data.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smedialink.bots.data.model.BotStatus
import com.smedialink.bots.data.model.BotType
import com.smedialink.bots.domain.model.BotLanguage
import java.util.*

class Converter {

    companion object {

        private val gson = Gson()

        @TypeConverter
        @JvmStatic
        fun toBotsDbModelStatus(name: String): BotStatus {
            return BotStatus.resolve(name)
        }

        @TypeConverter
        @JvmStatic
        fun toString(type: BotStatus): String {
            return type.name
        }

        @TypeConverter
        @JvmStatic
        fun toBotsDbModelType(name: String): BotType {
            return BotType.resolveByName(name)
        }


        @TypeConverter
        @JvmStatic
        fun toString(botLanguage: BotLanguage): String =
                botLanguage.langCode


        @TypeConverter
        @JvmStatic
        fun toBotLanguage(languageCode: String): BotLanguage =
                BotLanguage.fromValue(languageCode)

        @TypeConverter
        @JvmStatic
        fun toString(type: BotType): String {
            return type.name
        }

        @TypeConverter
        @JvmStatic
        fun fromStrings(value: List<String>?): String = gson.toJson(value)

        @TypeConverter
        @JvmStatic
        fun fromString(value: String?): List<String> {
            val listType = object : TypeToken<ArrayList<String>>() {}.type
            return value?.let { gson.fromJson<List<String>>(it, listType) } ?: emptyList()
        }

        @TypeConverter
        @JvmStatic
        fun fromTimeStamp(value: Long?): Date? = value?.let { Date(it) }

        @TypeConverter
        @JvmStatic
        fun fromDate(value: Date?): Long? = value?.time

        @TypeConverter
        @JvmStatic
        fun fromMap(value: Map<String,String>?): String =
                gson.toJson(value)

        @TypeConverter
        @JvmStatic
        fun toMap(value: String?): Map<String,String> {
            val listType = object : TypeToken<Map<String, String>>() {}.type
            return value?.let { gson.fromJson<Map<String, String>>(it, listType) } ?: emptyMap()
        }

    }
}
