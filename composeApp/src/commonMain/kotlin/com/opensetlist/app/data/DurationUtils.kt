package com.opensetlist.app.data

/**
 * Converte uma duração em texto ("3:15", "1:02:30") para segundos.
 *
 * @author ruanitto
 */
fun parseDurationSeconds(text: String): Long {
    val t = text.trim()
    if (t.isEmpty()) return 0
    val parts = t.split(":", limit = 3)
    val nums = parts.map { it.trim().toLongOrNull() ?: return 0 }
    return when (nums.size) {
        1 -> nums[0] * 60L
        2 -> nums[0] * 60L + nums[1]
        3 -> nums[0] * 3600L + nums[1] * 60L + nums[2]
        else -> 0
    }
}

/**
 * Formata segundos em uma duração legível ("3min", "1h 15min").
 *
 * @author ruanitto
 */
fun formatDuration(totalSeconds: Long): String {
    val secs = totalSeconds.coerceAtLeast(0)
    val hours = secs / 3600
    val minutes = (secs % 3600) / 60
    val seconds = secs % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}min"
        seconds > 0 -> "${seconds}s"
        else -> ""
    }
}

/**
 * Formata segundos no formato de relógio usado no campo duração ("4:20", "1:02:30").
 *
 * @author ruanitto
 */
fun formatSecondsClock(totalSeconds: Long): String {
    val secs = totalSeconds.coerceAtLeast(0)
    val hours = secs / 3600
    val minutes = (secs % 3600) / 60
    val seconds = secs % 60
    return when {
        hours > 0 ->
            "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        minutes > 0 -> "$minutes:${seconds.toString().padStart(2, '0')}"
        else -> "${seconds}s"
    }
}
