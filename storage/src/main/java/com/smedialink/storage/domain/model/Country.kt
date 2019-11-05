package com.smedialink.storage.domain.model

data class Country(val id: String,
                   val name: String,
                   val checked: Boolean = false,
                   val locales: Map<String, String>)