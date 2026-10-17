package com.rozakos.home.ui.util

import java.text.DateFormat
import java.util.Date

fun formatLastSeen(epochMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
    if (epochMillis == null) return "Never"
    val elapsed = (nowMillis - epochMillis).coerceAtLeast(0)
    return when {
        elapsed < 60_000 -> "Just now"
        elapsed < 3_600_000 -> "${elapsed / 60_000} min ago"
        elapsed < 86_400_000 -> "${elapsed / 3_600_000} h ago"
        else -> DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))
    }
}

fun normalizeIp(input: String): String =
    input.trim()
        .replace(Regex("^https?://", RegexOption.IGNORE_CASE), "")
        .substringBefore("/")

fun isLikelyIpv4(input: String): Boolean {
    val normalized = normalizeIp(input)
    val parts = normalized.split(".")
    return parts.size == 4 && parts.all { part ->
        part.length in 1..3 && part.all(Char::isDigit) && part.toIntOrNull()?.let { it in 0..255 } == true
    }
}
