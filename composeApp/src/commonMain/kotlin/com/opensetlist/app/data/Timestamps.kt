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

/**
 * Milissegundos desde a época Unix (1970-01-01T00:00:00Z).
 */
expect fun currentEpochMillis(): Long
