package com.opensetlist.app.data

import com.opensetlist.app.model.ChordProLine
import com.opensetlist.app.model.ChordProSegment
import com.opensetlist.app.model.ParsedSong

object ChordProParser {

    fun parse(body: String): ParsedSong {
        val meta = mutableMapOf<String, String>()
        val tags = mutableListOf<String>()
        val customMeta = mutableMapOf<String, String>()
        val lines = mutableListOf<ChordProLine>()

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
                                    commentStyle = resolved.commentStyle ?: com.opensetlist.app.model.CommentStyle.PLAIN
                                )
                            )
                        }
                        ChordProDirectives.Kind.SECTION_START -> {
                            val label = ChordProDirectives.sectionLabel(resolved, arg)
                            lines.add(
                                ChordProLine(
                                    segments = listOf(ChordProSegment(text = label)),
                                    isSection = true,
                                    sectionName = label
                                )
                            )
                        }
                        ChordProDirectives.Kind.SECTION_END -> Unit
                        ChordProDirectives.Kind.CHORUS -> {
                            lines.add(
                                ChordProLine(
                                    segments = listOf(ChordProSegment(text = "Chorus")),
                                    isSection = true,
                                    sectionName = "Chorus"
                                )
                            )
                        }
                        ChordProDirectives.Kind.IGNORE -> Unit
                    }
                } else if (ChordProDirectives.isConditionalName(rawName) && arg != null) {
                    lines.add(
                        ChordProLine(segments = listOf(ChordProSegment(text = arg)))
                    )
                } else if (arg == null) {
                    val label = rawName.trim()
                    lines.add(
                        ChordProLine(
                            segments = listOf(ChordProSegment(text = label)),
                            isSection = true,
                            sectionName = label
                        )
                    )
                }
            } else {
                lines.add(
                    if (rawLine.isBlank()) {
                        ChordProLine(segments = listOf(ChordProSegment(text = "")))
                    } else {
                        parseContentLine(rawLine, meta, tags, customMeta)
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
            sorttitle = meta["sorttitle"] ?: "",
            tags = tags,
            customMeta = customMeta,
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
