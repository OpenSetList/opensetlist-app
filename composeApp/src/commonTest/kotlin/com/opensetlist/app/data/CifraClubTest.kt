package com.opensetlist.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CifraClubTest {

    @Test
    fun slugify_simpleTitle() {
        assertEquals("que-pais-e-este", CifraClub.slugifySong("Que País É Este"))
    }

    @Test
    fun slugify_artist() {
        assertEquals("legiao-urbana", CifraClub.slugifyArtist("Legião Urbana"))
    }

    @Test
    fun slugify_ampersand_becomesE() {
        assertEquals("o-sol-e-a-lua", CifraClub.slugifySong("O Sol & a Lua"))
    }

    @Test
    fun slugify_removesPunctuation() {
        assertEquals("tempo-perdido", CifraClub.slugifySong("Tempo Perdido?"))
    }

    @Test
    fun titleMatches_ignoresCase() {
        assertTrue(CifraClub.titleMatches("Que País É Este", "que pais é este"))
    }

    @Test
    fun analyzeLine_chordOnlyLine() {
        val a = CifraClub.analyzeLine("<b>C</b>         <b>G</b>")
        assertTrue(a.isChordOnlyLine)
        assertEquals(listOf(0 to "C", 10 to "G"), a.chords)
    }

    @Test
    fun analyzeLine_mixedLine_excludesChordFromLyric() {
        val a = CifraClub.analyzeLine("Meu <b>Am</b> coração")
        assertFalse(a.isChordOnlyLine)
        assertEquals("Meu  coração", a.plain)
        assertEquals(listOf(4 to "Am"), a.chords)
    }

    @Test
    fun toChordProBody_mergesChordLineWithLyricLine() {
        val raw = "<b>C</b>         <b>G</b>\n" +
            "Quando eu era criança\n" +
            "<b>D</b>        <b>Em</b>\n" +
            "Nada do que eu sonhava"
        val body = CifraClub.toChordProBody(raw)
        val lines = body.lines()
        assertEquals("[C]Quando eu [G]era criança", lines[0])
        assertEquals("[D]Nada do [Em]que eu sonhava", lines[1])
    }

    @Test
    fun toChordProBody_keepsPlainLyricLines() {
        val body = CifraClub.toChordProBody("Uma linha sem acordes")
        assertEquals("Uma linha sem acordes", body)
    }

    @Test
    fun toChordProBody_replacesEntitiesAndBreaks() {
        val body = CifraClub.toChordProBody("Linha&nbsp;um<br>linha&nbsp;dois")
        assertEquals("Linha um\nlinha dois", body)
    }

    @Test
    fun parsePrintSheet_metadataAndBody() {
        val html = """
            <html><body>
            <h1>Que País É Este</h1>
            <h2>Legião Urbana</h2>
            <small>Composição de: Renato Russo</small>
            <button data-anchor="--chord-tone" class="songData">C</button>
            <button data-anchor="--chord-capo" class="songData">2ª casa</button>
            <pre class="SpJgV">
            <b>C</b>        <b>G</b>
            Quando as crianças
            <b>D</b>        <b>Em</b>
            Que tudo passou
            </pre>
            </body></html>
        """.trimIndent()
        val sheet = CifraClub.parsePrintSheet(html)

        assertEquals("Que País É Este", sheet?.title)
        assertEquals("Legião Urbana", sheet?.artist)
        assertEquals("Renato Russo", sheet?.composer)
        assertEquals("C", sheet?.key)
        assertEquals("2ª casa", sheet?.capo)
        assertTrue(sheet?.body?.contains("[C]") == true)
        assertTrue(sheet?.body?.contains("[D]") == true)
    }

    @Test
    fun parsePrintSheet_returnsNullWithoutPre() {
        val html = "<h1>Título</h1><p>sem acordes</p>"
        assertEquals(null, CifraClub.parsePrintSheet(html))
    }

    @Test
    fun parseArtistSongs_tileAnchor() {
        val html = """
            <div>
            <a href="/legiao-urbana/que-pais-e-este/" class="tile">
            <p class="primaryLabel"><span class="Jeax9">Que País É Este</span></p>
            <span class="secondaryLabel">42.1mi</span>
            <span class="tertiaryLabel">C</span>
            </a>
            <a href="/legiao-urbana/tempo-perdido/" class="tile">
            <p class="primaryLabel"><span class="Jeax9">Tempo Perdido</span></p>
            <span class="secondaryLabel">10mi</span>
            <span class="tertiaryLabel">Em</span>
            </a>
            <a href="/login">Login</a>
            </div>
        """.trimIndent()
        val songs = CifraClub.parseArtistSongs(html, "Legião Urbana")

        assertEquals(2, songs.size)
        assertEquals("Que País É Este", songs[0].title)
        assertEquals("Legião Urbana", songs[0].artist)
        assertEquals("https://www.cifraclub.com.br/legiao-urbana/que-pais-e-este/", songs[0].url)
        assertEquals("C", songs[0].key)
        assertEquals("42.1mi", songs[0].hits)
    }

    @Test
    fun toImportBody_buildsDirectives() {
        val sheet = CifraSheet(
            title = "Que País É Este",
            artist = "Legião Urbana",
            composer = "Renato Russo",
            key = "C",
            capo = "2ª casa",
            tuning = "",
            body = "[C]linha\nsegunda linha"
        )
        val body = sheet.toImportBody()
        assertTrue(body.startsWith("{title: Que País É Este}"))
        assertTrue(body.contains("{artist: Legião Urbana}"))
        assertTrue(body.contains("{composer: Renato Russo}"))
        assertTrue(body.contains("{key: C}"))
        assertTrue(body.contains("{capo: 2ª casa}"))
        assertTrue(body.contains("[C]linha"))
    }
}