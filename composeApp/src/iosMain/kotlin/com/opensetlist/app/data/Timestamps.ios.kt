package com.opensetlist.app.data

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale

private fun formatTimestamp(pattern: String): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = pattern
        locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
    }
    return formatter.stringFromDate(NSDate())
}

/**
 * Data e hora atual no formato ISO (ex.: "2026-07-31T14:30:00").
 *
 * @author ruanitto
 */
actual fun currentTimestampIso(): String = formatTimestamp("yyyy-MM-dd'T'HH:mm:ss")

/**
 * Data e hora atual segura para nomes de arquivo (ex.: "2026-07-31_14-30-00").
 */
actual fun currentTimestampCompact(): String = formatTimestamp("yyyy-MM-dd_HH-mm-ss")

/**
 * Milissegundos desde a época Unix.
 */
actual fun currentEpochMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
