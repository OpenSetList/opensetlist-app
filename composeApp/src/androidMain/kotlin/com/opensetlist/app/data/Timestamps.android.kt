package com.opensetlist.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data e hora atual no formato ISO (ex.: "2026-07-31T14:30:00").
 *
 * @author ruanitto
 */
actual fun currentTimestampIso(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).format(Date())

/**
 * Data e hora atual segura para nomes de arquivo (ex.: "2026-07-31_14-30-00").
 */
actual fun currentTimestampCompact(): String =
    SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(Date())
