package com.opensetlist.app.data

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone

/**
 * Deslocamento do fuso horário local no iOS (millis) no instante [utcTimeMillis].
 *
 * @author ruanitto
 */
actual fun utcOffsetMillis(utcTimeMillis: Long): Long =
    NSTimeZone.localTimeZone
        .secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(utcTimeMillis / 1000.0))
        .toLong() * 1000L
