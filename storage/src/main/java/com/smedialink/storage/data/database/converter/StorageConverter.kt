package com.smedialink.storage.data.database.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageConverter {

    companion object {

        @TypeConverter
        @JvmStatic
        fun toDialogsSet(str: String): Set<Long> {
            return gson.fromJson(str, setType)
        }

        @TypeConverter
        @JvmStatic
        fun toString(set: Set<Long>): String {
            return gson.toJson(set, setType)
        }

        @TypeConverter
        @JvmStatic
        fun toSrings(value: Set<String>?): String = gson.toJson(value)

        @TypeConverter
        @JvmStatic
        fun fromString(value: String?): Set<String> {
            val listType = object : TypeToken<Set<String>>() {}.type
            return value?.let { gson.fromJson<Set<String>>(it, listType) } ?: emptySet()
        }

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

        private val setType = object : TypeToken<Set<Long>>() {}.type
        private val gson: Gson = Gson()
    }
}
