package com.opensetlist.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChordProParserTest {

    @Test
    fun parse_metadataAndContent() {
        val body = "{title: Minha Música}\n{artist: Artista}\n{key: C}\n\n[G] Primeira linha\nSegunda linha"
        val parsed = ChordProParser.parse(body)

        assertEquals("Minha Música", parsed.title)
        assertEquals("Artista", parsed.artist)
        assertEquals("C", parsed.key)
    }

    @Test
    fun parse_inlineChord_segmented() {
        val body = "[G] só com acorde no início"
        val parsed = ChordProParser.parse(body)

        val line = parsed.lines.single()
        assertEquals("G", line.segments[0].chord)
        assertEquals("", line.segments[0].text)
        assertEquals(" só com acorde no início", line.segments[1].text)
    }

    @Test
    fun parse_chordInMiddle_preservesTextColumns() {
        val body = "Olá [C]mundo"
        val parsed = ChordProParser.parse(body)
        val line = parsed.lines.single()

        assertEquals(listOf("Olá ", "", "mundo"), line.segments.map { it.text })
        assertEquals(null, line.segments[0].chord)
        assertEquals("C", line.segments[1].chord)
        assertTrue(line.segments[2].chord == null)
    }

    @Test
    fun parse_chorusSection_flagsIsChorus() {
        val body = "{start_of_chorus}\nSolo [C] aqui\n{end_of_chorus}"
        val parsed = ChordProParser.parse(body)

        assertTrue(parsed.lines.filter { it.isChorus }.any { it.isSection })
        assertTrue(parsed.lines.any { it.isChorus && !it.isSection })
    }

    @Test
    fun parse_tabSection_flagsIsTab() {
        val body = "{start_of_tab}\nE|--0--1--3--|\nB|--1--1--3--|\n{end_of_tab}"
        val parsed = ChordProParser.parse(body)

        val tabLines = parsed.lines.filter { it.isTab }
        // Linha de abertura da seção + 2 linhas de conteúdo + linha em branco do fechamento.
        assertEquals(4, tabLines.size)
        assertEquals("E|--0--1--3--|", tabLines[1].segments.joinToString("") { it.text })
    }

    @Test
    fun parse_highlightBlock_flagsIsHighlight() {
        val body = "{soh}\nSolo com palm mute\n{eoh}"
        val parsed = ChordProParser.parse(body)

        assertTrue(parsed.lines.filter { it.isHighlight }.any { !it.isSection })
        val content = parsed.lines.first { it.isHighlight && !it.isSection }
        assertEquals("Solo com palm mute", content.segments.joinToString("") { it.text })
    }

    @Test
    fun parse_setChordProDirective_addsAndPreservesOthers() {
        val body = "{title: A}\n{key: C}\n[G]"
        val updated = setChordProDirective(body, "tempo", "120")
        assertTrue(updated.contains("{tempo: 120}"))
        assertTrue(updated.contains("{title: A}"))
        assertTrue(updated.contains("{key: C}"))
        assertTrue(updated.contains("[G]"))
    }

    @Test
    fun parse_setChordProDirective_updatesExisting() {
        val body = "{key: C}"
        val updated = setChordProDirective(body, "key", "D")
        assertEquals("{key: D}", updated)
    }

    @Test
    fun parse_setChordProDirective_removesWhenBlank() {
        val body = "{key: C}\n[G]"
        val updated = setChordProDirective(body, "key", "")
        assertTrue(!updated.contains("{key:"))
        assertTrue(updated.contains("[G]"))
    }

    @Test
    fun parse_comment_marksIsComment() {
        val body = "{comment: Nota do intérprete}"
        val parsed = ChordProParser.parse(body)
        assertTrue(parsed.lines.single().isComment)
        assertEquals("Nota do intérprete", parsed.lines.single().segments.joinToString("") { it.text })
    }
}
