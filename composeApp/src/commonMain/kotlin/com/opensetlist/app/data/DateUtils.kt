package com.opensetlist.app.data

/**
 * Conversão de datas no formato "dd/MM/yyyy" para o DatePicker do Material3 e vice-versa.
 *
 * O DatePicker trabalha com millis desde a época (epoch) e interpreta os valores no fuso
 * horário local do aparelho. Para não depender de fuso, usamos [utcOffsetMillis] para
 * traduzir entre "meia-noite UTC" e "meia-noite local".
 *
 * @author ruanitto
 */

internal data class SimpleDate(val year: Int, val month: Int, val day: Int)

internal fun parseSimpleDate(text: String): SimpleDate? {
    val parts = text.trim().split("/")
    if (parts.size != 3) return null
    val day = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    if (year !in 1..9999 || month !in 1..12 || day !in 1..31) return null
    return SimpleDate(year, month, day)
}

internal fun simpleDateToUtcMidnightMillis(date: SimpleDate): Long =
    daysFromCivil(date.year, date.month, date.day) * 86_400_000L

internal fun utcMidnightMillisToSimpleDate(millis: Long): SimpleDate {
    val (year, month, day) = civilFromDays(millis / 86_400_000L)
    return SimpleDate(year, month, day)
}

/** Converte "dd/MM/yyyy" nos millis esperados pelo [DatePickerState] (meia-noite local). */
fun toDatePickerMillis(dateText: String): Long? {
    val date = parseSimpleDate(dateText) ?: return null
    val utcMidnight = simpleDateToUtcMidnightMillis(date)
    return utcMidnight + utcOffsetMillis(utcMidnight)
}

/** Converte os millis retornados pelo [DatePickerState] de volta para "dd/MM/yyyy". */
fun fromDatePickerMillis(millis: Long): String {
    val utcMidnight = millis - utcOffsetMillis(millis)
    val date = utcMidnightMillisToSimpleDate(utcMidnight)
    return "${date.day.toString().padStart(2, '0')}/${date.month.toString().padStart(2, '0')}/${date.year}"
}

/** Formata epoch (ms) como "dd/MM/yyyy"; retorna "" quando inválido. */
fun formatEpochDate(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    return fromDatePickerMillis(epochMillis)
}

/** Formata epoch (ms) como "HH:mm"; retorna "" quando inválido. */
fun formatEpochTime(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    val localDay = epochMillis - utcOffsetMillis(epochMillis)
    val seconds = ((localDay % 86_400_000L) + 86_400_000L) % 86_400_000L
    val hour = (seconds / 3_600_000L).toInt()
    val minute = ((seconds % 3_600_000L) / 60_000L).toInt()
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

/** Formata epoch (ms) como "dd/MM/yyyy HH:mm" (apenas a data quando meia-noite). */
fun formatEpochDateTime(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    val date = formatEpochDate(epochMillis)
    val time = formatEpochTime(epochMillis)
    return if (time == "00:00") date else "$date $time"
}

/** Combina millis do DatePicker (meia-noite local) com hora e minuto em um epoch. */
fun combineDateAndTime(datePickerMillis: Long, hour: Int, minute: Int): Long =
    datePickerMillis + hour * 3_600_000L + minute * 60_000L

/** Converte "HH:mm" em hora e minuto; retorna null quando inválido. */
fun parseClockTime(text: String): Pair<Int, Int>? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

/** Deslocamento do fuso horário local (em millis) no instante [utcTimeMillis]. */
expect fun utcOffsetMillis(utcTimeMillis: Long): Long

/** Dias desde a época (algoritmo days_from_civil de Howard Hinnant). */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    var y = year.toLong()
    var m = month.toLong()
    if (m <= 2) y -= 1
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val mp = (m + 9) % 12
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** Data civil a partir de dias desde a época (algoritmo civil_from_days de Howard Hinnant). */
private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    var z = days + 719468
    val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    return Triple((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
}
