package com.opensetlist.app.data

import java.util.TimeZone

/**
 * Deslocamento do fuso horário local no desktop/JVM (millis) no instante [utcTimeMillis].
 *
 * @author ruanitto
 */
actual fun utcOffsetMillis(utcTimeMillis: Long): Long =
    TimeZone.getDefault().getOffset(utcTimeMillis).toLong()
