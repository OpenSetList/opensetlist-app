package com.opensetlist.app.data

/**
 * Resultado do parse do conteúdo de uma tab do Ultimate Guitar.
 */
internal data class UgParsedSheet(
    val body: String,
    val key: String,
    val capo: String,
    val tuning: String
)

/**
 * Cliente do Ultimate Guitar: busca e obtém tabs via scraping das páginas SSR.
 *
 * A busca usa `search.php?search_type=title&value=...` (a resposta vem em um
 * bloco JSON no atributo `data-content` do `div.js-store`), e cada tab é
 * carregada em `tabs.ultimate-guitar.com/tab/{id}`, onde o corpo fica em
 * `store.page.data.tab_view.wiki_tab.content`.
 */
object UltimateGuitar {

    private const val SEARCH_BASE = "https://www.ultimate-guitar.com/search.php"
    private const val MAX_RESULTS = 15
    private const val CHORD_CHARS = "0-9#+/øº°A-G-"

    private val UG_CHORD_TOKEN = Regex(
        "[A-G][#b]?(?:[$CHORD_CHARS]+(?:maj|sus|dim|aug|add)?[$CHORD_CHARS]*|m(?:[$CHORD_CHARS]*)?|(?:maj|sus|dim|aug|add)(?:[$CHORD_CHARS]*))?"
    )
    private val SECTION_LABEL = Regex("^\\s*\\[[^\\[\\]]+\\]\\s*$")
    private val TAB_ANNOTATION = Regex("\\s*\\([^)]*\\)\\s*")

    /**
     * Busca online: URL colada vira a tab direta; senão procura pelo título
     * (a parte após " - " quando o termo é "Artista - Música").
     */
    suspend fun search(
        query: String,
        fallbackArtist: String = "",
        fallbackTitle: String = ""
    ): CifraSearchOutcome {
        val input = query.trim()
        if (input.isBlank()) return CifraSearchOutcome.NoResult

        if (isUltimateGuitarUrl(input)) {
            val sheet = fetchSongByUrl(input)
            return if (sheet != null) CifraSearchOutcome.Sheet(sheet)
            else CifraSearchOutcome.NoResult
        }

        val title = when {
            input.contains(" - ") -> input.substringAfter(" - ").trim()
            fallbackTitle.isNotBlank() -> fallbackTitle
            else -> input
        }
        val songs = if (title.isBlank()) emptyList() else searchTabs(title)
        return if (songs.isNotEmpty()) CifraSearchOutcome.Songs(songs)
        else CifraSearchOutcome.NoResult
    }

    /** Busca as tabs por título e mantém apenas as de acordes. */
    suspend fun fetchSongByUrl(url: String): CifraSheet? {
        if (url.isBlank()) return null
        val html = CifraClub.httpGet(url) ?: return null
        return parseTabPage(html)
    }

    internal fun isUltimateGuitarUrl(input: String): Boolean =
        input.contains("ultimate-guitar.com")

    /**
     * Extrai as tabs de acordes (e ukulele) da resposta de busca, ignorando
     * tabs pagos (sem `id`) e tablaturas.
     */
    internal fun parseSearchResults(html: String): List<CifraSong> {
        val js = extractDataContent(html)?.replaceEntities() ?: return emptyList()
        val seen = mutableSetOf<String>()
        val out = mutableListOf<CifraSong>()
        var scan = 0
        while (true) {
            val idMatch = Regex("\"id\":(\\d+)").find(js, scan) ?: break
            val urlMatch = Regex("\"tab_url\":\"((?:\\\\.|[^\"\\\\])*)\"").find(js, idMatch.range.last + 1) ?: break
            val url = decodeJsonString(urlMatch.groupValues[1])
            scan = urlMatch.range.last + 1
            if (!url.startsWith("https://tabs.ultimate-guitar.com/tab/")) continue
            if (!seen.add(url)) continue
            val item = js.substring(idMatch.range.first, urlMatch.range.first)
            val title = jsonString(item, "song_name") ?: continue
            val artist = jsonString(item, "artist_name").orEmpty()
            val type = jsonString(item, "type").orEmpty()
            if (!type.contains("Chord", ignoreCase = true)) continue
            val tonality = jsonString(item, "tonality_name").orEmpty()
            out.add(CifraSong(title.trim(), artist.trim(), url, key = tonality))
            if (out.size >= MAX_RESULTS) break
        }
        return out
    }

    /** Converte a página da tab em [CifraSheet] com o corpo em ChordPro. */
    internal fun parseTabPage(html: String): CifraSheet? {
        val js = extractDataContent(html)?.replaceEntities() ?: return null
        val title = jsonString(js, "song_name")?.trim() ?: return null
        val artist = jsonString(js, "artist_name").orEmpty().trim()
        val content = jsonString(js, "content") ?: return null
        val parsed = ugContentToChordPro(content)
        if (parsed.body.isBlank()) return null
        return CifraSheet(title, artist, "", parsed.key, parsed.capo, parsed.tuning, parsed.body)
    }

    /**
     * Converte o conteúdo de uma tab do UG (formato `[ch][/ch]`/`[tab]`
     * `[/tab]`) em corpo ChordPro, detectando também linhas de acordes sem
     * marcação (ex.: `D5+`, ` Fº E7`) e removendo anotações `(com pestana)`.
     */
    internal fun ugContentToChordPro(raw: String): UgParsedSheet {
        var key = ""
        var capo = ""
        var tuning = ""
        val out = mutableListOf<String>()
        for (line0 in raw.lines()) {
            val line = line0.trimEnd()
            val t = line.trim()
            if (t.startsWith("Tom", ignoreCase = true) && t.contains(':')) {
                key = t.substringAfter(':').trim()
                continue
            }
            if (t.startsWith("Afinação", ignoreCase = true) && t.contains(':')) {
                tuning = t.substringAfter(':').trim()
                continue
            }
            if (t.startsWith("Capo", ignoreCase = true)) {
                capo = if (t.contains(':')) t.substringAfter(':').trim() else t.removePrefix("Capo").trim()
                continue
            }
            val clean = line.replace("[tab]", "").replace("[/tab]", "")
            val ct = clean.trim()
            when {
                clean.contains("[ch]") -> {
                    out.add(clean.replace(Regex("\\[ch\\](.*?)\\[/ch\\]")) { "[${it.groupValues[1].trim()}]" })
                }
                isBareChordLine(ct) -> out.add(wrapBareChordLine(clean))
                SECTION_LABEL.matches(ct) -> out.add("{comment: ${ct.removeSurrounding("[", "]")}}")
                else -> out.add(clean)
            }
        }
        return UgParsedSheet(
            body = out.joinToString("\n").trim(),
            key = key,
            capo = capo,
            tuning = tuning
        )
    }

    private suspend fun searchTabs(title: String): List<CifraSong> {
        val html = CifraClub.httpGet("$SEARCH_BASE?search_type=title&value=${CifraClub.urlEncode(title)}")
            ?: return emptyList()
        return parseSearchResults(html)
    }

    private fun extractDataContent(html: String): String? =
        Regex("<div[^>]*class=\"js-store\"[^>]*data-content=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)

    private fun jsonString(json: String, key: String): String? {
        val marker = "\"$key\":\""
        val i = json.indexOf(marker)
        if (i < 0) return null
        val start = i + marker.length
        val end = jsonEnd(json, start) ?: return null
        return decodeJsonString(json.substring(start, end))
    }

    /** Índice da aspa que fecha o valor JSON começando em [from] (suporta escapes). */
    private fun jsonEnd(s: String, from: Int): Int? {
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (c == '\\') i += 2 else if (c == '"') return i else i++
        }
        return null
    }

    private fun decodeJsonString(raw: String): String {
        val sb = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                when (val next = raw[i + 1]) {
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '/' -> { sb.append('/'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000C'); i += 2 }
                    'u' -> {
                        val end = minOf(i + 6, raw.length)
                        val hex = raw.substring(i + 2, end)
                        val cp = hex.toIntOrNull(16)?.toChar()
                        sb.append(cp ?: '\\')
                        i = if (cp != null) i + 2 + hex.length else i + 2
                    }
                    else -> { sb.append(c); i++ }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun isBareChordLine(clean: String): Boolean {
        val tokens = TAB_ANNOTATION.replace(clean, " ").split(Regex("\\s+")).filter { it.isNotBlank() }
        return tokens.isNotEmpty() && tokens.all { UG_CHORD_TOKEN.matches(it) }
    }

    private fun wrapBareChordLine(line: String): String {
        val out = StringBuilder()
        var pos = 0
        for (m in UG_CHORD_TOKEN.findAll(line)) {
            out.append(line, pos, m.range.first)
            out.append('[').append(m.value).append(']')
            pos = m.range.last + 1
        }
        out.append(line, pos, line.length)
        return TAB_ANNOTATION.replace(out.toString(), "")
    }
}