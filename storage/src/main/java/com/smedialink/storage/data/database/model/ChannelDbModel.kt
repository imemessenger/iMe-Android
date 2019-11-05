package com.smedialink.storage.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class ChannelDbModel(@PrimaryKey
                          @ColumnInfo(name = "id")
                          val id: String,
                          @ColumnInfo(name = "title")
                          val title: String,
                          @ColumnInfo(name = "subscribers")
                          val subscribers: Int,
                          @ColumnInfo(name = "joined")
                          var joined: Boolean = false,
                          @ColumnInfo(name = "imageUrl")
                          val imageUrl: String,
                          @ColumnInfo(name = "description")
                          val description: String,
                          @ColumnInfo(name = "tags")
                          val tags: Set<String> = emptySet(),
                          @ColumnInfo(name = "countries")
                          val countries: Set<String> = emptySet(),
                          @ColumnInfo(name = "reviews")
                          val reviews: Long = 0,
                          @ColumnInfo(name = "rating")
                          val rating: Float = 0f,
                          @ColumnInfo(name = "ownRating")
                          var ownRating: Int = 0)