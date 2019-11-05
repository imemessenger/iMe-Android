package com.smedialink.storage.domain.model

data class Channel(val id: String,
                   val title: String,
                   val subscribers: Int,
                   var joined: Boolean = false,
                   val imageUrl: String,
                   val rating: Float = 0f,
                   val ownRating: Int=0,
                   val reviews: Int = 0,
                   val description: String,
                   val tags: Set<String> = emptySet(),
                   val countries: Set<String> = emptySet()) {

    val searchField: String
        get() = id + title + description

}