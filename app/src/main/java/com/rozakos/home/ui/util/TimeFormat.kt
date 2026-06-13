package com.rozakos.home.ui.util

import java.text.DateFormat
import java.util.Date

fun formatLastSeen(epochMillis: Long?): String {
    if (epochMillis == null) return "Never seen"
    return "Last seen ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))}"
}

fun normalizeIp(input: String): String =
    input.trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore("/")

fun isLikelyIpv4(input: String): Boolean {
    val normalized = normalizeIp(input)
    val parts = normalized.split(".")
    return parts.size == 4 && parts.all { part ->
        part.toIntOrNull()?.let { it in 0..255 } == true
    }
}
