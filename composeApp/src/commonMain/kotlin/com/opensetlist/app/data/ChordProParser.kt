package com.opensetlist.app.data

import com.opensetlist.app.model.ChordProLine
import com.opensetlist.app.model.ChordProSegment
import com.opensetlist.app.model.ParsedSong

/**
 * Parseador do formato ChordPro, transformando o texto bruto em uma [ParsedSong].
 *
 * @author ruanitto
 */
object ChordProParser {

    fun parse(body: String): ParsedSong {
        val meta = mutableMapOf<String, String>()
        val tags = mutableListOf<String>()
        val customMeta = mutableMapOf<String, String>()
        val lines = mutableListOf<ChordProLine>()
        var inChorus = false
        var inTab = false
        var inHighlight = false

        fun ChordProLine.withState() = copy(
            isChorus = inChorus,
            isTab = inTab,
            isHighlight = inHighlight
        )

        for (rawLine in body.lines()) {
            val directive = tryParseDirective(rawLine)
            if (directive != null) {
                val (rawName, arg) = directive
                val resolved = ChordProDirectives.resolve(rawName)

                if (resolved != null) {
                    when (resolved.kind) {
                        ChordProDirectives.Kind.METADATA -> {
                            val value = arg ?: ""
                            applyMetadata(resolved.metadataKey ?: resolved.name, value, meta, tags, customMeta)
                        }
                        ChordProDirectives.Kind.COMMENT -> {
                            val label = arg ?: ""
                            lines.add(
                                ChordProLine(
                                    segments = listOf(ChordProSegment(text = label)),
                                    isComment = true,
                                    commentStyle = resolved.commentStyle ?: com.opensetlist.app.model.CommentStyle.PLAIN,
                                    isChorus = inChorus
                                ).copy(
                                    isTab = inTab,
                                    isHighlight = inHighlight
                                )
                            )
                        }
                        ChordProDirectives.Kind.SECTION_START -> {
                            inChorus = resolved.name == "start_of_chorus"
                            inTab = resolved.name == "start_of_tab"
                            inHighlight = resolved.name == "start_of_highlight"
                            val label = ChordProDirectives.sectionLabel(resolved, arg)
                            lines.add(
                                ChordProLine(
                                    segments = listOf(ChordProSegment(text = label)),
                                    isSection = true,
                                    sectionName = label,
                                    isChorus = inChorus
                                ).copy(
                                    isTab = inTab,
                                    isHighlight = inHighlight
                                )
                            )
                        }
                        ChordProDirectives.Kind.SECTION_END -> {
                            val wasTab = inTab
                            val wasHighlight = inHighlight
                            inChorus = if (resolved.name == "end_of_chorus") false else inChorus
                            inTab = if (resolved.name == "end_of_tab") false else inTab
                            inHighlight = if (resolved.name == "end_of_highlight") false else inHighlight
                            if (wasTab || wasHighlight) {
                                lines.add(
                                    ChordProLine(
                                        segments = listOf(ChordProSegment(text = "")),
                                        isChorus = inChorus,
                                        isTab = wasTab,
                                        isHighlight = wasHighlight
                                    )
                                )
                            }
                        }
                        ChordProDirectives.Kind.CHORUS -> {
                            val label = ChordProDirectives.sectionLabel(resolved, arg)
                            lines.add(
                                ChordProLine(
                                    segments = listOf(ChordProSegment(text = label)),
                                    isSection = true,
                                    sectionName = label,
                                    isChorus = true
                                ).copy(
                                    isTab = inTab,
                                    isHighlight = inHighlight
                                )
                            )
                        }
                        ChordProDirectives.Kind.IGNORE -> Unit
                    }
                } else if (ChordProDirectives.isConditionalName(rawName) && arg != null) {
                    lines.add(
                        ChordProLine(
                            segments = listOf(ChordProSegment(text = arg)),
                            isChorus = inChorus
                        ).copy(
                            isTab = inTab,
                            isHighlight = inHighlight
                        )
                    )
                } else if (arg == null) {
                    val label = rawName.trim()
                    lines.add(
                        ChordProLine(
                            segments = listOf(ChordProSegment(text = label)),
                            isSection = true,
                            sectionName = label,
                            isChorus = inChorus
                        ).copy(
                            isTab = inTab,
                            isHighlight = inHighlight
                        )
                    )
                }
            } else {
                lines.add(
                    if (rawLine.isBlank()) {
                        ChordProLine(
                            segments = listOf(ChordProSegment(text = "")),
                            isChorus = inChorus
                        ).copy(
                            isTab = inTab,
                            isHighlight = inHighlight
                        )
                    } else {
                        parseContentLine(rawLine, meta, tags, customMeta).withState()
                    }
                )
            }
        }

        return ParsedSong(
            title = meta["title"] ?: "",
            subtitle = meta["subtitle"] ?: "",
            artist = meta["artist"] ?: "",
            composer = meta["composer"] ?: "",
            lyricist = meta["lyricist"] ?: "",
            copyright = meta["copyright"] ?: "",
            album = meta["album"] ?: "",
            year = meta["year"] ?: "",
            key = meta["key"] ?: "",
            time = meta["time"] ?: "",
            tempo = meta["tempo"] ?: "",
            duration = meta["duration"] ?: "",
            capo = meta["capo"] ?: "",
            youtube = meta["youtube"] ?: "",
            sorttitle = meta["sorttitle"] ?: "",
            tags = tags,
            customMeta = customMeta,
            transpose = meta["transpose"]?.toIntOrNull() ?: 0,
            lines = lines
        )
    }

    private fun tryParseDirective(line: String): Pair<String, String?>? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        val inner = trimmed.removeSurrounding("{", "}")
        if (inner.isBlank()) return null

        var nameEnd = 0
        while (nameEnd < inner.length && inner[nameEnd] != ':' && !inner[nameEnd].isWhitespace()) {
            nameEnd++
        }
        if (nameEnd == 0) return null
        val name = inner.substring(0, nameEnd)
        val rest = inner.substring(nameEnd)
        val arg = when {
            rest.isEmpty() -> null
            rest[0] == ':' -> rest.substring(1).trim().ifEmpty { null }
            else -> rest.trim().ifEmpty { null }
        }
        return name to arg
    }

    private fun parseContentLine(
        line: String,
        meta: MutableMap<String, String>,
        tags: MutableList<String>,
        customMeta: MutableMap<String, String>
    ): ChordProLine {
        val segments = mutableListOf<ChordProSegment>()
        val current = StringBuilder()
        var i = 0

        fun flushText() {
            if (current.isNotEmpty()) {
                segments.add(ChordProSegment(text = current.toString()))
                current.clear()
            }
        }

        while (i < line.length) {
            val c = line[i]
            when {
                c == '[' -> {
                    val chordEnd = line.indexOf(']', i)
                    if (chordEnd != -1) {
                        flushText()
                        segments.add(ChordProSegment(text = "", chord = line.substring(i + 1, chordEnd)))
                        i = chordEnd + 1
                    } else {
                        current.append(c)
                        i++
                    }
                }
                c == '{' -> {
                    val tokenEnd = line.indexOf('}', i)
                    if (tokenEnd == -1) {
                        current.append(c)
                        i++
                        continue
                    }
                    val token = line.substring(i + 1, tokenEnd)
                    var nameEnd = 0
                    while (nameEnd < token.length && token[nameEnd] != ':' && !token[nameEnd].isWhitespace()) {
                        nameEnd++
                    }
                    val rawName = token.substring(0, nameEnd)
                    val arg = if (nameEnd < token.length) {
                        token.substring(nameEnd).removePrefix(":").trim().ifEmpty { null }
                    } else {
                        null
                    }

                    val resolved = ChordProDirectives.resolve(rawName)
                    when {
                        resolved != null -> {
                            when (resolved.kind) {
                                ChordProDirectives.Kind.METADATA -> {
                                    val value = arg ?: ""
                                    applyMetadata(resolved.metadataKey ?: resolved.name, value, meta, tags, customMeta)
                                }
                                ChordProDirectives.Kind.COMMENT -> {
                                    if (!arg.isNullOrBlank()) current.append(arg)
                                }
                                else -> Unit
                            }
                            i = tokenEnd + 1
                        }
                        ChordProDirectives.isConditionalName(rawName) && arg != null -> {
                            current.append(arg)
                            i = tokenEnd + 1
                        }
                        else -> {
                            current.append(c)
                            i++
                        }
                    }
                }
                else -> {
                    current.append(c)
                    i++
                }
            }
        }

        flushText()
        return ChordProLine(segments = segments)
    }

    private fun applyMetadata(
        key: String,
        value: String,
        meta: MutableMap<String, String>,
        tags: MutableList<String>,
        customMeta: MutableMap<String, String>
    ) {
        meta[key] = value
        when (key) {
            "tag" -> tags.add(value)
            "meta" -> {
                val parts = value.split(" ", limit = 2)
                if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                    customMeta[parts[0]] = parts.getOrElse(1) { "" }
                }
            }
        }
    }
}

/** Insere, atualiza ou remove a diretiva [name] no corpo ChordPro. */
fun setChordProDirective(body: String, name: String, value: String?): String {
    val isBlank = value.isNullOrBlank()
    if (body.isBlank() && isBlank) return body
    val lines = body.lines()
    val index = lines.indexOfFirst { line -> directiveNameOf(line) == name }
    return when {
        isBlank && index == -1 -> body
        isBlank -> lines.filterIndexed { i, _ -> i != index }.joinToString("\n")
        index == -1 -> "{$name: $value}\n$body"
        else -> lines.mapIndexed { i, line -> if (i == index) "{$name: $value}" else line }.joinToString("\n")
    }
}

private fun directiveNameOf(line: String): String? {
    val trimmed = line.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
    val inner = trimmed.removeSurrounding("{", "}")
    var nameEnd = 0
    while (nameEnd < inner.length && inner[nameEnd] != ':' && !inner[nameEnd].isWhitespace()) {
        nameEnd++
    }
    if (nameEnd == 0) return null
    return inner.substring(0, nameEnd).trim().lowercase()
}
