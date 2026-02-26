package com.example.mobile_tracker.domain.model

data class Employee(
    val id: String,
    val fullName: String,
    val companyName: String? = null,
    val position: String? = null,
    val passNumber: String? = null,
    val personnelNumber: String? = null,
    val brigadeName: String? = null,
    val siteId: String? = null,
)
