package com.example.mobile_tracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Long.toLocalDateTime(
    zone: ZoneId = ZoneId.systemDefault(),
): LocalDateTime = LocalDateTime.ofInstant(
    Instant.ofEpochMilli(this),
    zone,
)

fun Long.toFormattedDate(
    pattern: String = "dd.MM.yyyy",
    zone: ZoneId = ZoneId.systemDefault(),
): String = toLocalDateTime(zone).format(
    DateTimeFormatter.ofPattern(pattern),
)

fun Long.toFormattedDateTime(
    pattern: String = "dd.MM.yyyy HH:mm",
    zone: ZoneId = ZoneId.systemDefault(),
): String = toLocalDateTime(zone).format(
    DateTimeFormatter.ofPattern(pattern),
)

fun LocalDate.toIsoString(): String =
    format(DateTimeFormatter.ISO_LOCAL_DATE)

fun formatTimestamp(
    millis: Long,
    pattern: String = "HH:mm:ss",
    zone: ZoneId = ZoneId.systemDefault(),
): String = millis.toLocalDateTime(zone).format(
    DateTimeFormatter.ofPattern(pattern),
)
