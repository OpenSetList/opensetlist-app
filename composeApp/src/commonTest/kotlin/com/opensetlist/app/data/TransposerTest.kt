package com.opensetlist.app.data

import kotlin.test.Test
import kotlin.test.assertEquals

class TransposerTest {

    @Test
    fun transposeChord_zeroSemitones_returnsSame() {
        assertEquals("Am7", Transposer.transposeChord("Am7", 0))
        assertEquals("", Transposer.transposeChord("", 4))
    }

    @Test
    fun transposeChord_blank_returnsSame() {
        assertEquals("   ", Transposer.transposeChord("   ", 3))
    }

    @Test
    fun transposeChord_major_positive() {
        assertEquals("D", Transposer.transposeChord("C", 2))
        assertEquals("E", Transposer.transposeChord("C", 4))
    }

    @Test
    fun transposeChord_major_negative() {
        assertEquals("B", Transposer.transposeChord("C", -1))
        assertEquals("G", Transposer.transposeChord("C", -5))
    }

    @Test
    fun transposeChord_suffixPreserved() {
        assertEquals("Bm7", Transposer.transposeChord("Am7", 2))
        assertEquals("Asus4", Transposer.transposeChord("Gsus4", 2))
        assertEquals("F#sus4", Transposer.transposeChord("Gsus4", -1))
    }

    @Test
    fun transposeChord_wrapsAroundOctave() {
        assertEquals("C", Transposer.transposeChord("B", 1))
        assertEquals("B", Transposer.transposeChord("C", -1))
        assertEquals("C", Transposer.transposeChord("C", 12))
    }

    @Test
    fun transposeChord_flatRoot_preservesFlatNotation() {
        assertEquals("C", Transposer.transposeChord("Bb", 2))
        assertEquals("A", Transposer.transposeChord("Bb", -1))
        assertEquals("F", Transposer.transposeChord("Eb", 2))
    }

    @Test
    fun transposeChord_sharpRoot_preservesSharp() {
        assertEquals("D#maj7", Transposer.transposeChord("C#maj7", 2))
        assertEquals("G#", Transposer.transposeChord("F#", 2))
    }

    @Test
    fun transposeChord_bassNoteAlsoTransposed() {
        assertEquals("D/F#", Transposer.transposeChord("C/E", 2))
        assertEquals("B/D#", Transposer.transposeChord("C/E", -1))
        assertEquals("F#7/A#", Transposer.transposeChord("E7/G#", 2))
        assertEquals("E/G#", Transposer.transposeChord("D/F#", 2))
    }

    @Test
    fun transposeChord_unknownNote_returnsSame() {
        assertEquals("Hmaj", Transposer.transposeChord("Hmaj", 2))
    }

    @Test
    fun transposeBody_transposesInlineChordsAndKey() {
        val body = "{key: C}\n[G] [Am7] [C/E]"
        val expected = "{key: D}\n[A] [Bm7] [D/F#]"
        assertEquals(expected, Transposer.transposeBody(body, 2))
    }

    @Test
    fun transposeBody_keyDirectiveOnly_noChords() {
        assertEquals("{key: F}", Transposer.transposeBody("{key: C}", 5))
    }

    @Test
    fun transposeBody_zeroSemitones_returnsSame() {
        val body = "{key: C}\n[G]"
        assertEquals(body, Transposer.transposeBody(body, 0))
    }
}
