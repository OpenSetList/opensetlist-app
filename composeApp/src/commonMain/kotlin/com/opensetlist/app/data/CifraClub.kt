package com.opensetlist.app.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/**
 * Resultado de busca no Cifra Club: um item da lista de músicas de um artista.
 */
data class CifraSong(
    val title: String,
    val artist: String,
    val url: String,
    val key: String = "",
    val hits: String = ""
)

/**
 * Cifra completa parseada (corpo em ChordPro + metadados).
 */
data class CifraSheet(
    val title: String,
    val artist: String,
    val composer: String,
    val key: String,
    val capo: String,
    val tuning: String,
    val body: String
)

/**
 * Resultado intermediário de uma busca online.
 */
sealed class CifraSearchOutcome {
    data class Sheet(val sheet: CifraSheet) : CifraSearchOutcome()
    data class Songs(val songs: List<CifraSong>) : CifraSearchOutcome()
    data object NoResult : CifraSearchOutcome()
}

/** Converte uma cifra obtida da internet em corpo ChordPro com as diretivas de metadados. */
fun CifraSheet.toImportBody(): String {
    val sb = StringBuilder()
    if (title.isNotBlank()) sb.appendLine("{title: $title}")
    if (artist.isNotBlank() && artist != "Artista") sb.appendLine("{artist: $artist}")
    if (composer.isNotBlank()) sb.appendLine("{composer: $composer}")
    if (key.isNotBlank()) sb.appendLine("{key: $key}")
    if (capo.isNotBlank()) sb.appendLine("{capo: $capo}")
    val trimmedBody = body.trim()
    if (tuning.isNotBlank() && trimmedBody.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("(afinação: $tuning)")
    }
    if (trimmedBody.isNotEmpty()) {
        sb.appendLine()
        sb.append(trimmedBody)
    }
    return sb.toString()
}

/**
 * Cliente do Cifra Club: busca e obtém cifras via scraping das páginas SSR.
 *
 * A busca tradicional (`/busca/`) deixou de existir, então a busca usa URL por
 * slug ou a lista de músicas do artista.
 */
object CifraClub {

    private const val BASE_URL = "https://www.cifraclub.com.br"
    private const val USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"
    private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.9"

    private val client: HttpClient by lazy { buildHttpClient() }

    private const val INVALID_SONG_PATH = "(cifras|letras|estilos|tools|dicionario|login|artistas|menos|noticias|videoaulas|produtos|treino|conta)"
    private val WEB_IGNORED_SUFFIX = setOf("letra", "imprimir", "acordes", "instrumental", "versao", "traducao")

    /** Normaliza um texto (minúsculas, sem acentos) para comparação de títulos. */
    fun normalize(text: String): String =
        text.lowercase().trim().map { removeAccent(it) }.joinToString("")

    /** Gera o slug usado pelo Cifra Club a partir do nome do artista. */
    fun slugifyArtist(name: String): String = slugify(name)

    /** Gera o slug usado pelo Cifra Club a partir do título da música. */
    fun slugifySong(title: String): String = slugify(title)

    /** Verifica se a entrada parece uma URL de cifra do Cifra Club. */
    fun looksLikeUrl(input: String): Boolean =
        input.contains("cifraclub.com.br") || input.startsWith("/")

    /** Percent-encode UTF-8 de um texto (portável, sem java.net.URLEncoder). */
    fun urlEncode(text: String): String {
        val hex = "0123456789ABCDEF"
        val sb = StringBuilder()
        for (byte in text.encodeToByteArray()) {
            val value = byte.toUByte().toInt()
            val c = value.toChar()
            if (value in 'a'.code..'z'.code || value in 'A'.code..'Z'.code ||
                value in '0'.code..'9'.code || c == '-' || c == '_' || c == '.' || c == '~'
            ) {
                sb.append(c)
            } else {
                sb.append('%')
                    .append(hex[value shr 4])
                    .append(hex[value and 0x0F])
            }
        }
        return sb.toString()
    }

    /** URL de busca no Google para o termo informado. */
    fun googleSearchUrl(query: String): String =
        "https://www.google.com/search?q=${urlEncode(query)}"

    /**
     * Busca online: tenta URL colada, depois slug artista+música, depois a lista
     * de músicas do artista e, por fim, uma busca web (DuckDuckGo) apontando
     * para o Cifra Club — útil quando só o nome da música é informado.
     */
    suspend fun search(
        query: String,
        fallbackArtist: String = "",
        fallbackTitle: String = ""
    ): CifraSearchOutcome {
        val input = query.trim()
        if (input.isBlank()) return CifraSearchOutcome.NoResult

        if (looksLikeUrl(input)) {
            val sheet = fetchSongByUrl(input)
            return if (sheet != null) CifraSearchOutcome.Sheet(sheet)
            else CifraSearchOutcome.NoResult
        }

        val sep = input.indexOf(" - ")
        val artist: String
        val title: String
        if (sep != -1) {
            artist = input.substring(0, sep).trim()
            title = input.substring(sep + 3).trim()
        } else if (fallbackArtist.isNotBlank()) {
            artist = fallbackArtist
            title = input
        } else {
            artist = input
            title = ""
        }

        if (artist.isNotBlank() && title.isNotBlank()) {
            val sheet = fetchSongBySlugs(artist, title)
            if (sheet != null && titleMatches(sheet.title, title)) {
                return CifraSearchOutcome.Sheet(sheet)
            }
        }

        if (artist.isNotBlank()) {
            val songs = fetchArtistSongs(artist)
            val filtered = if (title.isBlank()) songs
            else songs.filter { titleMatches(it.title, title) }
            if (filtered.isNotEmpty()) return CifraSearchOutcome.Songs(filtered)
        }

        val webHits = resolveViaWeb(input)
        return if (webHits.isNotEmpty()) CifraSearchOutcome.Songs(webHits)
        else CifraSearchOutcome.NoResult
    }

    /** Busca links de cifras do Cifra Club para o termo informado (Brave e, como
     *  segundo plano, DuckDuckGo — quando o termo é só título, sem artista). */
    internal suspend fun resolveViaWeb(query: String): List<CifraSong> {
        val term = query.trim()
        if (term.isBlank()) return emptyList()
        val brave = httpGet("https://search.brave.com/search?q=${urlEncode("\"$term\" cifra club")}")
            ?.let(::parseBraveHits)
            .orEmpty()
        if (brave.isNotEmpty()) return brave
        val ddg = httpGet("https://html.duckduckgo.com/html/?q=${urlEncode("site:cifraclub.com.br \"$term\"")}")
            ?.let(::parseWebHits)
            .orEmpty()
        return ddg
    }

    internal fun parseBraveHits(html: String): List<CifraSong> {
        val pair = Regex("\\{[^}]*title:\"([^\"]+)\"[^}]*url:\"(https://www\\.cifraclub\\.com\\.br/[^\"]+)\"[^}]*\\}")
        val seen = mutableSetOf<String>()
        val result = mutableListOf<CifraSong>()
        for (match in pair.findAll(html)) {
            val titleText = match.groupValues[1]
                .replace("\\u0026", "&")
                .replaceEntities()
                .trim()
            val normalized = normalizeSongUrl(match.groupValues[2]) ?: continue
            if (!seen.add(normalized)) continue
            result.add(parseWebTitle(titleText, normalized))
        }
        return result
    }

    internal fun parseWebHits(html: String): List<CifraSong> {
        val anchorPattern = Regex(
            "<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>([\\s\\S]*?)</a>",
            RegexOption.DOT_MATCHES_ALL
        )
        val seen = mutableSetOf<String>()
        val result = mutableListOf<CifraSong>()
        for (match in anchorPattern.findAll(html)) {
            val rawUrl = match.groupValues[1]
            val target = urlDecode(rawUrl.substringAfter("uddg=").substringBefore('&'))
            val normalized = normalizeSongUrl(target) ?: continue
            if (!seen.add(normalized)) continue
            val titleText = match.groupValues[2]
                .replace(Regex("<[^>]*>"), "")
                .replaceEntities()
                .replace(" - ", " - ")
                .trim()
            result.add(parseWebTitle(titleText, normalized))
        }
        return result
    }

    private fun normalizeSongUrl(raw: String): String? {
        if (!raw.contains("cifraclub.com.br")) return null
        var path = raw
            .substringAfter("cifraclub.com.br", missingDelimiterValue = raw)
            .substringBefore("?")
            .let { if (it.startsWith('/')) it else "/$it" }
        if (path.length <= 1) return null
        val segments = path.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null
        val song = segments.last().trim()
        if (song in WEB_IGNORED_SUFFIX || segments.first().matches(Regex("^$INVALID_SONG_PATH$"))) return null
        return "cifraclub.com.br/${segments[0]}/${song}/"
    }

    private fun parseWebTitle(text: String, normalizedUrl: String): CifraSong {
        val clean = text
            .replace(Regex("\\s*\\(letra da m\\u00fasica\\)\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*\\|\\s*CIFRAS\\s*$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s*-\\s*Cifra Club\\s*$", RegexOption.IGNORE_CASE), "")
            .trim()
        val parts = clean.split(Regex("\\s*-\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        val title: String
        val artist: String
        if (parts.size >= 2) {
            artist = parts.last()
            title = parts.dropLast(1).joinToString(" - ")
        } else {
            title = clean
            val segments = normalizedUrl.split('/').filter { it.isNotBlank() }
            artist = segments.getOrNull(1).orEmpty()
        }
        return CifraSong(
            title = title,
            artist = artist,
            url = "https://www.${normalizedUrl}",
            key = "",
            hits = ""
        )
    }

    /** Decodifica percent-encoding (sem java.net.URLDecoder). */
    internal fun urlDecode(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '+' -> {
                    sb.append(' ')
                    i++
                }
                c == '%' && i + 3 <= text.length -> {
                    val value = text.substring(i + 1, i + 3).toIntOrNull(16)
                    if (value != null) {
                        sb.append(value.toChar())
                        i += 3
                    } else {
                        sb.append(c)
                        i++
                    }
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        return sb.toString()
    }

    internal fun titleMatches(songTitle: String, search: String): Boolean {
        val a = normalize(songTitle)
        val b = normalize(search)
        return a == b || a.contains(b) || b.contains(a)
    }

    /** Obtém a cifra a partir de uma URL colada (da própria página ou da de impressão). */
    suspend fun fetchSongByUrl(url: String): CifraSheet? {
        val clean = url.trim().substringBefore("?")
        val base = clean
            .let { if (it.endsWith("imprimir.html")) it.substringBefore("imprimir.html") else it }
            .let { if (it.endsWith("/")) it else "$it/" }
        return fetchSong("${base}imprimir.html") ?: fetchSong(base)
    }

    /** Tenta a URL direta `/<artista>/<música>/` (sem precisar listar o artista). */
    suspend fun fetchSongBySlugs(artistName: String, title: String): CifraSheet? {
        val artistSlug = slugifyArtist(artistName)
        val songSlug = slugifySong(title)
        if (artistSlug.isBlank() || songSlug.isBlank()) return null
        return fetchSong("$BASE_URL/$artistSlug/$songSlug/imprimir.html")
    }

    /** Lista as músicas da página de um artista. */
    suspend fun fetchArtistSongs(artistName: String): List<CifraSong> {
        val artistSlug = slugifyArtist(artistName)
        if (artistSlug.isBlank()) return emptyList()
        val html = httpGet("$BASE_URL/$artistSlug/") ?: return emptyList()
        return parseArtistSongs(html, artistName)
    }

    private suspend fun fetchSong(url: String): CifraSheet? {
        val html = httpGet(url) ?: return null
        return parsePrintSheet(html) ?: parseRegularSheet(html)
    }

    suspend fun httpGet(url: String): String? =
        runCatching {
            val response = client.get(url) {
                header("User-Agent", USER_AGENT)
                header("Accept-Language", ACCEPT_LANGUAGE)
            }
            if (response.status.value in 200..299) response.bodyAsText() else null
        }.getOrNull()

    /**
     * Converte o HTML da página de impressão (`/<artista>/<música>/imprimir.html`)
     * em uma [CifraSheet] com corpo ChordPro.
     */
    internal fun parsePrintSheet(html: String): CifraSheet? {
        val clean = stripHtmlComments(html)
        val title = firstTagText(clean, "h1") ?: return null
        val artist = firstTagText(clean, "h2").orEmpty().takeIf { it.isNotBlank() }
            ?: "Artista"
        val composer = Regex(
            "Composi(?:ç(?:ã|&#227;)o|&#231;(?:ã|&#227;)o|cao)\\s*(?:de:)?\\s*(.*?)</small>",
            RegexOption.IGNORE_CASE
        ).find(clean)?.groupValues?.getOrNull(1)?.trim() ?: ""
        val key = dataAnchor(clean, "--chord-tone")?.trim()?.split(Regex("\\s+"))?.firstOrNull() ?: ""
        val capo = dataAnchor(clean, "--chord-capo").orEmpty().trim()
        val tuning = dataAnchor(clean, "--chord-tuning").orEmpty().trim()

        val body = toChordProBody(extractPreText(clean))
        if (body.isBlank()) return null

        return CifraSheet(title, artist, composer, key, capo, tuning, body)
    }

    /**
     * Fallback: página normal da música (`/<artista>/<música>/`).
     */
    internal fun parseRegularSheet(html: String): CifraSheet? {
        val clean = stripHtmlComments(html)
        val title = firstTagText(clean, "h1") ?: return null
        val artist = firstTagText(clean, "h2").orEmpty().takeIf { it.isNotBlank() } ?: "Artista"
        val key = dataAnchor(clean, "--chord-tone")?.trim()?.split(Regex("\\s+"))?.firstOrNull() ?: ""
        val capo = regexFirst(
            clean,
            Regex("<[^>]*id=\"capo\"[^>]*>[\\s\\S]*?<p[^>]*>([^<]*)</p>", RegexOption.IGNORE_CASE)
        )?.trim().orEmpty()
        val tuning = dataAnchor(clean, "--chord-tuning").orEmpty().trim()

        val raw = regexFirst(clean, Regex("<pre[^>]*>(.*?)</pre>", RegexOption.DOT_MATCHES_ALL))
            ?: return null
        val body = toChordProBody(raw)
        if (body.isBlank()) return null

        return CifraSheet(title, artist, "", key, capo, tuning, body)
    }

    private fun stripHtmlComments(html: String): String =
        html.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")

    private fun extractPreText(html: String): String {
        val prePattern = Regex("<pre[^>]*>(.*?)</pre>", RegexOption.DOT_MATCHES_ALL)
        return prePattern.findAll(html).map { it.groupValues.getOrNull(1).orEmpty() }
            .joinToString("\n")
    }

    private fun firstTagText(html: String, tag: String): String? =
        regexFirst(html, Regex("<$tag[^>]*>(.*?)</$tag>", RegexOption.DOT_MATCHES_ALL))?.trim()

    private fun dataAnchor(html: String, anchor: String): String? =
        regexFirst(html, Regex("<button[^>]*data-anchor=\"$anchor\"[^>]*>(.*?)</button>", RegexOption.DOT_MATCHES_ALL))

    private fun regexFirst(html: String, regex: Regex): String? =
        regex.find(html)?.groupValues?.getOrNull(1)

    private fun slugify(value: String): String {
        val cleaned = value.lowercase().trim().replace('&', 'e').replace('ª', ' ').replace('º', ' ')
        val builder = StringBuilder()
        for (c in cleaned) builder.append(removeAccent(c))
        return builder.toString()
            .fold(StringBuilder()) { acc, c ->
                if (c in 'a'..'z' || c in '0'..'9') acc.append(c)
                else if (acc.isEmpty() || acc.last() != '-') acc.append('-')
                acc
            }
            .trim('-')
            .toString()
    }

    /** Remove acentos de letras latinas (Latin-1 Supplement + Latin Extended-A). */
    private fun removeAccent(c: Char): Char {
        val code = c.code
        val base = when {
            code == 0x00C6 || code == 0x00E6 || code in 0x00C0..0x00C5 || code in 0x00E0..0x00E5 ||
                code in 0x0100..0x0105 -> 'a'
            code == 0x00C7 || code == 0x00E7 || code in 0x0106..0x010D -> 'c'
            code in 0x010E..0x0111 -> 'd'
            code in 0x00C8..0x00CB || code in 0x00E8..0x00EB || code in 0x0112..0x011B -> 'e'
            code in 0x011C..0x0123 -> 'g'
            code in 0x0124..0x0127 -> 'h'
            code in 0x00CC..0x00CF || code in 0x00EC..0x00EF || code in 0x0128..0x012F -> 'i'
            code == 0x0134 || code == 0x0135 -> 'j'
            code == 0x0136 || code == 0x0137 || code == 0x0138 -> 'k'
            code in 0x0139..0x0142 -> 'l'
            code == 0x00D1 || code == 0x00F1 || code in 0x0143..0x014B -> 'n'
            code in 0x00D2..0x00D6 || code in 0x00F2..0x00F6 || code == 0x00D8 || code == 0x00F8 ||
                code in 0x014C..0x0153 -> 'o'
            code in 0x0154..0x0159 -> 'r'
            code == 0x00DF || code in 0x015A..0x0161 -> 's'
            code in 0x0162..0x0167 -> 't'
            code in 0x00D9..0x00DC || code in 0x00F9..0x00FC || code in 0x0168..0x0173 -> 'u'
            code == 0x0174 || code == 0x0175 -> 'w'
            code == 0x00DD || code == 0x00FD || code == 0x00FF || code in 0x0176..0x0178 -> 'y'
            code in 0x0179..0x017E -> 'z'
            else -> return c
        }
        return if (c.isUpperCase()) base.uppercaseChar() else base
    }

    /**
     * Transforma o texto bruto da cifra (com `<b>` para acordes) em corpo ChordPro
     * com acordes `[Acorde]` alinhados sobre a letra.
     */
    internal fun toChordProBody(raw: String): String {
        val normalized = raw
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replaceEntities()
        val lines = normalized.split("\n")
        val out = StringBuilder()
        var pending: List<Pair<Int, String>>? = null

        for (rawLine in lines) {
            val a = analyzeLine(rawLine)
            when {
                a.chords.isEmpty() && a.plain.isBlank() -> {
                    flushPending(out, pending)
                    pending = null
                    out.appendLine()
                }
                a.chords.isNotEmpty() && a.isChordOnlyLine -> {
                    pending = a.chords
                }
                pending != null -> {
                    out.appendLine(alignToChordPro(pending, a.plain))
                    pending = null
                }
                a.chords.isNotEmpty() -> {
                    out.appendLine(alignToChordPro(a.chords, a.plain))
                }
                else -> {
                    out.appendLine(a.plain)
                }
            }
        }
        flushPending(out, pending)
        return out.toString().trimEnd('\n')
    }

    private fun flushPending(out: StringBuilder, pending: List<Pair<Int, String>>?) {
        if (pending != null && pending.isNotEmpty()) {
            out.appendLine(pending.joinToString(" ") { "[${it.second}]" })
        }
    }

    /** Analisa uma linha crua da cifra e devolve o texto plano + acordes por coluna. */
    internal fun analyzeLine(raw: String): LineAnalysis {
        val chordPattern = Regex("<b[^>]*>([^<]*)</b>", RegexOption.IGNORE_CASE)
        val plain = StringBuilder()
        val chords = mutableListOf<Pair<Int, String>>()
        var col = 0
        var i = 0
        while (i < raw.length) {
            val match = chordPattern.find(raw, i)
            if (match != null) {
                val before = raw.substring(i, match.range.first).replace(Regex("<[^>]*>"), "")
                plain.append(before)
                col += before.length
                val chord = match.groupValues.getOrNull(1).orEmpty()
                chords.add(col to chord)
                col += chord.length
                i = match.range.last + 1
            } else {
                val rest = raw.substring(i).replace(Regex("<[^>]*>"), "")
                plain.append(rest)
                break
            }
        }
        val isChordOnlyLine = chords.isNotEmpty() &&
            plain.toString().none { !it.isWhitespace() }
        return LineAnalysis(plain.toString(), chords, isChordOnlyLine)
    }

    data class LineAnalysis(
        val plain: String,
        val chords: List<Pair<Int, String>>,
        val isChordOnlyLine: Boolean
    )

    /** Alinha acordes posicionados por coluna acima/ao longo da linha de letra. */
    internal fun alignToChordPro(chords: List<Pair<Int, String>>, lyric: String): String {
        if (chords.isEmpty()) return lyric
        val insertions = chords.map { (col, chord) ->
            chordTargetInLyric(col, lyric) to chord
        }
        return buildWithInsertions(lyric, insertions)
    }

    private fun buildWithInsertions(lyric: String, insertions: List<Pair<Int, String>>): String {
        val ordered = insertions.sortedBy { it.first }
        val out = StringBuilder()
        var pos = 0
        for ((target, chord) in ordered) {
            val safeTarget = target.coerceIn(0, lyric.length)
            if (safeTarget >= pos) {
                out.append(lyric, pos, safeTarget)
                pos = safeTarget
            }
            out.append('[').append(chord).append(']')
        }
        out.append(lyric, pos, lyric.length)
        return out.toString()
    }

    private fun chordTargetInLyric(col: Int, lyric: String): Int {
        if (lyric.isBlank()) return 0
        var target = col.coerceIn(0, lyric.length)
        if (target == lyric.length) {
            val last = lyric.indexOfLast { !it.isWhitespace() }
            return if (last < 0) 0 else wordStart(lyric, last)
        }
        if (lyric[target].isWhitespace()) {
            while (target < lyric.length && lyric[target].isWhitespace()) target++
            if (target == lyric.length) {
                val last = lyric.indexOfLast { !it.isWhitespace() }
                return if (last < 0) 0 else wordStart(lyric, last)
            }
        } else {
            while (target > 0 && !lyric[target - 1].isWhitespace()) target--
        }
        return target
    }

    private fun wordStart(lyric: String, from: Int): Int {
        var i = from
        while (i > 0 && !lyric[i - 1].isWhitespace()) i--
        return i
    }

    private fun String.replaceEntities(): String {
        val sb = StringBuilder()
        var i = 0
        while (i < length) {
            val c = this[i]
            if (c == '&') {
                val semi = indexOf(';', i)
                if (semi != -1 && semi - i <= 12) {
                    val entity = substring(i + 1, semi)
                    val decoded: Char? = when {
                        entity.startsWith("#x") || entity.startsWith("#X") ->
                            entity.substring(2).toIntOrNull(16)?.toChar()
                        entity.startsWith("#") ->
                            entity.substring(1).toIntOrNull()?.toChar()
                        else -> when (entity) {
                            "nbsp" -> ' '
                            "lt" -> '<'
                            "gt" -> '>'
                            "amp" -> '&'
                            "quot" -> '"'
                            "apos" -> '\''
                            else -> null
                        }
                    }
                    if (decoded != null) {
                        sb.append(decoded)
                        i = semi + 1
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /** Extrai as músicas (tiles) da página de um artista. */
    internal fun parseArtistSongs(html: String, artistLabel: String = ""): List<CifraSong> {
        val anchorPattern = Regex(
            "<a[^>]+href=\"(/([a-z0-9-]+)/([a-z0-9-]+)/)\"[^>]*>([\\s\\S]*?)</a>",
            RegexOption.IGNORE_CASE
        )
        val seen = mutableSetOf<String>()
        val result = mutableListOf<CifraSong>()
        for (match in anchorPattern.findAll(html)) {
            val url = match.groupValues[1]
            val songSlug = match.groupValues[3]
            if (songSlug.matches(Regex("^$INVALID_SONG_PATH$"))) continue
            if (!seen.add(url)) continue
            val inner = match.groupValues[4]
            val title = tileTitle(inner) ?: continue
            val key = regexFirst(
                inner,
                Regex("<span[^>]*class=\"[^\"]*(?:[Tt]ertiaryLabel|tom)[^\"]*\"[^>]*>([^<]*)</span>")
            )?.trim() ?: ""
            val hits = regexFirst(
                inner,
                Regex("<span[^>]*class=\"[^\"]*[Ss]econdaryLabel[^\"]*\"[^>]*>([^<]*)</span>")
            )?.trim() ?: ""
            result.add(
                CifraSong(
                    title = title,
                    artist = artistLabel.ifBlank { songSlug },
                    url = "$BASE_URL$url",
                    key = key,
                    hits = hits
                )
            )
        }
        return result
    }

    private fun tileTitle(inner: String): String? {
        regexFirst(
            inner,
            Regex("<p[^>]*class=\"[^\"]*[Pp]rimaryLabel[^\"]*\"[^>]*>[\\s\\S]*?<span[^>]*>([^<]*)</span>")
        )?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        regexFirst(
            inner,
            Regex("<h2[^>]*>([^<]*)</h2>")
        )?.takeIf { it.isNotBlank() }?.let { return it.trim() }
        return null
    }
}