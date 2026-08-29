package com.opensetlist.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UltimateGuitarTest {

    /** Monta o HTML real do UG: JSON dentro de `data-content` com entidades HTML. */
    private fun jsStore(elements: String): String {
        val escaped = elements
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "&quot;")
        return "<div class=\"js-store\" data-content=\"$escaped\">"
    }

    @Test
    fun parseSearchResults_extractsChordTabsOnly() {
        val html = jsStore(
            """{"store":{"page":{"data":{"search_query":"a maca","results":[
                {"id":1107198,"song_id":277780,"song_name":"A Maçã","artist_id":34341,"artist_name":"Raul Seixas","type":"Chords","rating":4.62,"tonality_name":"","tab_url":"https://tabs.ultimate-guitar.com/tab/raul-seixas/a-maca-chords-1107198"},
                {"id":1366455,"song_name":"A Maçã","artist_name":"Raul Seixas","type":"Ukulele Chords","tab_url":"https://tabs.ultimate-guitar.com/tab/raul-seixas/a-maca-ukulele-1366455"},
                {"song_name":"Mach 5","artist_name":"The Presidents of the United States of America","tab_url":"https://www.ultimate-guitar.com/pro/?tab_id=314277"},
                {"id":5415840,"song_name":"Asphalt 8","artist_name":"MACAN","type":"Tabs","tab_url":"https://tabs.ultimate-guitar.com/tab/macan/asphalt-8-tabs-5415840"}
            ]}}}}
            """
        )
        val songs = UltimateGuitar.parseSearchResults(html)

        assertEquals(2, songs.size)
        assertEquals("A Maçã", songs[0].title)
        assertEquals("Raul Seixas", songs[0].artist)
        assertEquals("https://tabs.ultimate-guitar.com/tab/raul-seixas/a-maca-chords-1107198", songs[0].url)
        assertEquals("A Maçã", songs[1].title)
        assertTrue(songs[1].url.contains("a-maca-ukulele-1366455"))
    }

    @Test
    fun parseSearchResults_extractsTonality() {
        val html = jsStore(
            """{"store":{"page":{"data":{"results":[
                {"id":5415840,"song_name":"Asphalt 8","artist_name":"MACAN","type":"Chords","tonality_name":"Bm","tab_url":"https://tabs.ultimate-guitar.com/tab/macan/asphalt-8-chords-5415840"}
            ]}}}}
            """
        )
        val songs = UltimateGuitar.parseSearchResults(html)
        assertEquals("Bm", songs.single().key)
    }

    @Test
    fun parseSearchResults_emptyWithoutJsStore() {
        assertEquals(emptyList<CifraSong>(), UltimateGuitar.parseSearchResults("<html><body>buscando</body></html>"))
    }

    @Test
    fun parseTabPage_titleArtistAndChordProBody() {
        val html = jsStore(
            """
            {"store":{"page":{"data":{
                "tab":{"id":1107198,"song_name":"A Maçã","artist_name":"Raul Seixas","type":"Chords"},
                "tab_view":{"wiki_tab":{"content":"Tom: G
[tab][ch]Em[/ch]             [ch]Am[/ch]              [ch]D7[/ch]
Se esse amor ficar entre nós dois[/tab]
D5+
E um amor a dois profana
[Intro]
O ciúme é só vaidade"}}
            }}}}
            """.trimIndent()
        )
        val sheet = UltimateGuitar.parseTabPage(html)

        assertEquals("A Maçã", sheet?.title)
        assertEquals("Raul Seixas", sheet?.artist)
        assertEquals("G", sheet?.key)
        assertTrue(sheet?.body?.startsWith("[Em]             [Am]              [D7]") == true)
        assertTrue(sheet?.body?.contains("Se esse amor ficar entre nós dois") == true)
        assertTrue(sheet?.body?.contains("[D5+]") == true)
        assertTrue(sheet?.body?.contains("{comment: Intro}") == true)
    }

    @Test
    fun parseTabPage_nullWithoutContent() {
        val html = jsStore(
            """{"store":{"page":{"data":{"tab":{"song_name":"Sem corpo"},"tab_view":{}}}}}"""
        )
        assertNull(UltimateGuitar.parseTabPage(html))
    }

    @Test
    fun ugContentToChordPro_keyMetadataAndBareChords() {
        val content = "Tom: G\r\n                \r\n" +
            "[tab][ch]Em[/ch]             [ch]Am[/ch]              [ch]D7[/ch]\r\n" +
            "Se esse amor ficar entre nós dois[/tab]\r\n" +
            "D5+\r\n" +
            "E um amor a dois profana\r\n" +
            " Fº             E7\r\n" +
            "Irá gostar de todas\r\n" +
            "D7                      G (com pestana)\r\n" +
            "Sofro mas eu vou te libertar"
        val parsed = UltimateGuitar.ugContentToChordPro(content)
        val body = parsed.body.lines()

        assertEquals("G", parsed.key)
        assertEquals("[Em]             [Am]              [D7]", body[0])
        assertEquals("Se esse amor ficar entre nós dois", body[1])
        assertEquals("[D5+]", body[2])
        assertEquals("E um amor a dois profana", body[3])
        assertTrue(body[4].contains("[Fº]"))
        assertTrue(body[4].contains("[E7]"))
        assertEquals("Irá gostar de todas", body[5])
        assertEquals("[D7]                      [G]", body[6])
        assertEquals("Sofro mas eu vou te libertar", body[7])
        assertFalse(parsed.body.contains("(com pestana)"))
        assertFalse(parsed.body.contains("[tab]"))
    }

    @Test
    fun ugContentToChordPro_sectionLabelsAndMetadata() {
        val content = "Tom: E\nAfinação: Eb Ab Db Gb Bb Eb\nCapo: 2\n[Intro]\n[Refrão]\nUma frase"
        val parsed = UltimateGuitar.ugContentToChordPro(content)

        assertEquals("E", parsed.key)
        assertEquals("Eb Ab Db Gb Bb Eb", parsed.tuning)
        assertEquals("2", parsed.capo)
        assertEquals("{comment: Intro}", parsed.body.lines()[0])
        assertEquals("{comment: Refrão}", parsed.body.lines()[1])
        assertEquals("Uma frase", parsed.body.lines()[2])
    }

    @Test
    fun ugContentToChordPro_keepsPlainLyrics() {
        val parsed = UltimateGuitar.ugContentToChordPro("Letra solta sem acordes aqui")
        assertEquals("Letra solta sem acordes aqui", parsed.body)
    }

    @Test
    fun parseTabPage_largeContentDoesNotOverflowStack() {
        val longContent = buildString {
            repeat(4000) { appendLine("[ch]Em[/ch]      [ch]Am[/ch]      [ch]D7[/ch]")
                appendLine("Só mais uma vez eu tento te esquecer") }
        }
        val html = jsStore(
            """{"store":{"page":{"data":{
                "tab":{"song_name":"Longa","artist_name":"Artista"},
                "tab_view":{"wiki_tab":{"content":"$longContent"}}}
            }}}""".trimIndent()
        )
        val sheet = UltimateGuitar.parseTabPage(html)
        assertTrue(sheet?.body?.length ?: 0 > 100_000)
        assertTrue(sheet?.body?.contains("[Em]") == true)
        assertEquals(8000, sheet?.body?.lines()?.size)
    }
}