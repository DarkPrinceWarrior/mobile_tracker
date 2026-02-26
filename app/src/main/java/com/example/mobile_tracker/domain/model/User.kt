package com.example.mobile_tracker.domain.model

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val scopeType: String,
    val scopeIds: List<String>,
)
