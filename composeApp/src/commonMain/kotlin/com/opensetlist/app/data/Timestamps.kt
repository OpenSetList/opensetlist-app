package com.opensetlist.app.data

/**
 * Data e hora atual no formato ISO (ex.: "2026-07-31T14:30:00").
 *
 * @author ruanitto
 */
expect fun currentTimestampIso(): String

/**
 * Data e hora atual segura para nomes de arquivo (ex.: "2026-07-31_14-30-00").
 */
expect fun currentTimestampCompact(): String
