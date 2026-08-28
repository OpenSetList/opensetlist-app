package com.opensetlist.app.data

import com.opensetlist.app.model.CommentStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChordProDirectivesTest {

    @Test
    fun resolve_metadata_aliases() {
        assertEquals("title", ChordProDirectives.resolve("title")?.name)
        assertEquals("title", ChordProDirectives.resolve("t")?.name)
        assertEquals("key", ChordProDirectives.resolve("tom")?.name)
        assertEquals("tempo", ChordProDirectives.resolve("bpm")?.name)
        assertEquals("duration", ChordProDirectives.resolve("duração")?.name)
    }

    @Test
    fun resolve_sections() {
        assertEquals("start_of_chorus", ChordProDirectives.resolve("soc")?.name)
        assertEquals("end_of_chorus", ChordProDirectives.resolve("eoc")?.name)
        assertEquals("start_of_tab", ChordProDirectives.resolve("sot")?.name)
        assertEquals("end_of_tab", ChordProDirectives.resolve("eot")?.name)
        assertEquals("start_of_highlight", ChordProDirectives.resolve("soh")?.name)
        assertEquals("end_of_highlight", ChordProDirectives.resolve("eoh")?.name)
    }

    @Test
    fun resolve_commentStyles() {
        assertEquals(CommentStyle.ITALIC, ChordProDirectives.resolve("comment_italic")?.commentStyle)
        assertEquals(CommentStyle.BOX, ChordProDirectives.resolve("comment_box")?.commentStyle)
        assertEquals(CommentStyle.HIGHLIGHT, ChordProDirectives.resolve("highlight")?.commentStyle)
        assertEquals(CommentStyle.PLAIN, ChordProDirectives.resolve("comment")?.commentStyle)
    }

    @Test
    fun resolve_unknown_returnsNull() {
        assertNull(ChordProDirectives.resolve("not_a_directive"))
        assertNull(ChordProDirectives.resolve(""))
    }

    @Test
    fun resolve_caseInsensitive() {
        assertEquals("key", ChordProDirectives.resolve("KEY")?.name)
        assertEquals("chorus", ChordProDirectives.resolve("Chorus")?.name)
    }

    @Test
    fun resolve_extensionIgnored() {
        assertNotNull(ChordProDirectives.resolve("x_custom"))
        assertEquals("x_", ChordProDirectives.resolve("x_custom")?.name)
    }

    @Test
    fun isConditionalName_numbers() {
        assertTrue(ChordProDirectives.isConditionalName("1"))
        assertTrue(ChordProDirectives.isConditionalName("2+"))
        assertTrue(ChordProDirectives.isConditionalName("12"))
        assertTrue(!ChordProDirectives.isConditionalName("verse"))
    }

    @Test
    fun sectionLabel_prefersExplicitLabel() {
        val verse = ChordProDirectives.resolve("start_of_verse")!!
        assertEquals("Verso 1", ChordProDirectives.sectionLabel(verse, "label=\"Verso 1\""))
        assertEquals("Algo", ChordProDirectives.sectionLabel(verse, "Algo"))
    }

    @Test
    fun sectionLabel_fallsBackToDefault() {
        val chorus = ChordProDirectives.resolve("start_of_chorus")!!
        assertEquals("Chorus", ChordProDirectives.sectionLabel(chorus, null))
    }
}
