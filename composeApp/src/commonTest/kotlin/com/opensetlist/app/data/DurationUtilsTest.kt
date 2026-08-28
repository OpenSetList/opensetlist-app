package com.opensetlist.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DurationUtilsTest {

    @Test
    fun parseDuration_blank_returnsZero() {
        assertEquals(0L, parseDurationSeconds(""))
        assertEquals(0L, parseDurationSeconds("   "))
    }

    @Test
    fun parseDuration_secondsOnly_returnsMinutes() {
        assertEquals(3 * 60L, parseDurationSeconds("3"))
    }

    @Test
    fun parseDuration_minutesSeconds() {
        assertEquals(3 * 60L + 15, parseDurationSeconds("3:15"))
        assertEquals(45L, parseDurationSeconds("0:45"))
    }

    @Test
    fun parseDuration_hoursMinutesSeconds() {
        assertEquals(1L * 3600 + 2 * 60 + 30, parseDurationSeconds("1:02:30"))
    }

    @Test
    fun parseDuration_invalid_returnsZero() {
        assertEquals(0L, parseDurationSeconds("abc"))
        assertEquals(0L, parseDurationSeconds("1:x"))
        assertEquals(0L, parseDurationSeconds("a:b:c"))
    }

    @Test
    fun formatDuration_hoursAndMinutes() {
        assertEquals("1h 15min", formatDuration(60 * 60 + 15 * 60))
        assertEquals("2h", formatDuration(2 * 3600))
    }

    @Test
    fun formatDuration_minutesAndSeconds() {
        assertEquals("45min", formatDuration(45 * 60))
        assertEquals("30s", formatDuration(30))
        assertEquals("", formatDuration(0))
    }

    @Test
    fun formatSecondsClock_negative_coercesToZero() {
        assertEquals("", formatDuration(-10))
    }

    @Test
    fun formatSecondsClock_formatsClock() {
        assertEquals("4:20", formatSecondsClock(4 * 60 + 20))
        assertEquals("1:02:30", formatSecondsClock(3600 + 150))
        assertEquals("45s", formatSecondsClock(45))
    }
}
