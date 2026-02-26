package com.example.mobile_tracker.domain.model

data class Site(
    val id: String,
    val name: String,
    val address: String? = null,
    val timezone: String = "Europe/Moscow",
    val status: String = "active",
)
