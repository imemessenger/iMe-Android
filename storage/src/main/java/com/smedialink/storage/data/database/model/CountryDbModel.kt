package com.smedialink.storage.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CountryDbModel(@PrimaryKey
                          @ColumnInfo(name = "id")
                          val id: String,
                          @ColumnInfo(name = "name")
                          val name: String,
                          @ColumnInfo(name = "locales")
                          val locales: Map<String, String>)