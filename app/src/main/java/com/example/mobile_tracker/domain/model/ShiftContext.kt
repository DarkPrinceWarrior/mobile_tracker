package com.example.mobile_tracker.domain.model

data class ShiftContext(
    val siteId: String,
    val siteName: String,
    val shiftDate: String,
    val shiftType: String = "day",
    val operatorId: String,
    val operatorName: String,
)
