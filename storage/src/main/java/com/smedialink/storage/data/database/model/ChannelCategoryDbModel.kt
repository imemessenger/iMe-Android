package com.smedialink.storage.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChannelCategoryDbModel(@PrimaryKey
                                  @ColumnInfo(name = "id")
                                  val id: String,
                                  @ColumnInfo(name = "title")
                                  val title: String,
                                  @ColumnInfo(name = "tags")
                                  val tags: Set<String>,
                                  @ColumnInfo(name = "locales")
                                  val locales: Map<String, String>)