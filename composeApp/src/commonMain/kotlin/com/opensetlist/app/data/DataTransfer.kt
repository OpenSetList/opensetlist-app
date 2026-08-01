package com.opensetlist.app.data

import com.opensetlist.app.model.BackupData
import com.opensetlist.app.model.SetShareData
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.SetlistSongLink
import com.opensetlist.app.model.Song

/**
 * Serialização e detecção dos formatos de transferência de dados do app.
 *
 * @author ruanitto
 */
object DataTransfer {

    private const val TYPE_BACKUP = "setlist_app_backup"
    private const val TYPE_SONGS = "setlist_app_songs"
    private const val TYPE_SET = "setlist_app_set"

    fun detectType(json: String): String? {
        val parsed = JsonParser(json).parseObject() ?: return null
        val type = parsed["type"] as? String ?: return null
        return when (type) {
            TYPE_BACKUP -> "backup"
            TYPE_SONGS -> "songs"
            TYPE_SET -> "set"
            else -> null
        }
    }

    fun buildBackupJson(data: BackupData): String {
        val sb = StringBuilder()
        sb.append("{\"type\":\"$TYPE_BACKUP\",\"version\":2,\"createdAt\":")
        sb.append(quote(currentTimestampIso()))
        sb.append(",\"songs\":[")
        data.songs.forEachIndexed { i, s ->
            if (i > 0) sb.append(",")
            sb.append(songToJson(s))
        }
        sb.append("],\"setlists\":[")
        data.setlists.forEachIndexed { i, l ->
            if (i > 0) sb.append(",")
            sb.append(setlistToJson(l))
        }
        sb.append("],\"links\":[")
        data.links.forEachIndexed { i, l ->
            if (i > 0) sb.append(",")
            sb.append("{\"setlistId\":").append(quote(l.setlistId))
                .append(",\"songId\":").append(quote(l.songId))
                .append(",\"position\":").append(l.position).append("}")
        }
        sb.append("]}")
        return sb.toString()
    }

    fun parseBackupJson(json: String): BackupData? {
        val parsed = JsonParser(json).parseObject() ?: return null
        if (parsed["type"] != TYPE_BACKUP) return null

        val songs = mutableListOf<Song>()
        (parsed["songs"] as? List<*>)?.forEach { raw ->
            (raw as? Map<*, *>)?.let { songs.add(songFromJson(it)) }
        }

        val setlists = mutableListOf<Setlist>()
        (parsed["setlists"] as? List<*>)?.forEach { raw ->
            (raw as? Map<*, *>)?.let { setlists.add(setlistFromJson(it)) }
        }

        val links = mutableListOf<SetlistSongLink>()
        (parsed["links"] as? List<*>)?.forEach { raw ->
            val map = raw as? Map<*, *> ?: return@forEach
            val setlistId = map["setlistId"] as? String ?: return@forEach
            val songId = map["songId"] as? String ?: return@forEach
            links.add(
                SetlistSongLink(
                    setlistId = setlistId,
                    songId = songId,
                    position = (map["position"] as? Number)?.toInt() ?: 0
                )
            )
        }

        return BackupData(songs, setlists, links)
    }

    fun buildSongsBundleJson(songs: List<Song>): String {
        val sb = StringBuilder()
        sb.append("{\"type\":\"$TYPE_SONGS\",\"version\":1,\"songs\":[")
        songs.forEachIndexed { i, s ->
            if (i > 0) sb.append(",")
            sb.append(songToJson(s))
        }
        sb.append("]}")
        return sb.toString()
    }

    fun parseSongsBundleJson(json: String): List<Song>? {
        val parsed = JsonParser(json).parseObject() ?: return null
        if (parsed["type"] != TYPE_SONGS) return null
        val songs = mutableListOf<Song>()
        (parsed["songs"] as? List<*>)?.forEach { raw ->
            (raw as? Map<*, *>)?.let { songs.add(songFromJson(it)) }
        }
        return songs
    }

    fun buildSetJson(setlist: Setlist, songs: List<Song>): String {
        val sb = StringBuilder()
        sb.append("{\"type\":\"$TYPE_SET\",\"version\":1,\"setlist\":")
        sb.append(setlistToJson(setlist))
        sb.append(",\"songs\":[")
        songs.forEachIndexed { i, s ->
            if (i > 0) sb.append(",")
            sb.append(songToJson(s))
        }
        sb.append("]}")
        return sb.toString()
    }

    fun parseSetJson(json: String): SetShareData? {
        val parsed = JsonParser(json).parseObject() ?: return null
        if (parsed["type"] != TYPE_SET) return null
        val setlist = (parsed["setlist"] as? Map<*, *>)?.let { setlistFromJson(it) }
            ?: return null
        val songs = mutableListOf<Song>()
        (parsed["songs"] as? List<*>)?.forEach { raw ->
            (raw as? Map<*, *>)?.let { songs.add(songFromJson(it)) }
        }
        return SetShareData(setlist, songs)
    }

    private fun songToJson(s: Song): String {
        return "{\"id\":${quote(s.id)}," +
            "\"title\":${quote(s.title)}," +
            "\"artist\":${quote(s.artist)}," +
            "\"key\":${quote(s.key)}," +
            "\"tempo\":${quote(s.tempo)}," +
            "\"capo\":${quote(s.capo)}," +
            "\"duration\":${quote(s.duration)}," +
            "\"time\":${quote(s.time)}," +
            "\"youtubeUrl\":${quote(s.youtubeUrl)}," +
            "\"body\":${quote(s.body)}}"
    }

    private fun songFromJson(m: Map<*, *>): Song = Song(
        id = (m["id"] as? String) ?: "",
        title = (m["title"] as? String) ?: "",
        artist = (m["artist"] as? String) ?: "",
        key = (m["key"] as? String) ?: "",
        tempo = (m["tempo"] as? String) ?: "",
        capo = (m["capo"] as? String) ?: "",
        duration = (m["duration"] as? String) ?: "",
        time = (m["time"] as? String) ?: "",
        youtubeUrl = (m["youtubeUrl"] as? String) ?: "",
        body = (m["body"] as? String) ?: ""
    )

    private fun setlistToJson(l: Setlist): String {
        return "{\"id\":${quote(l.id)}," +
            "\"name\":${quote(l.name)}," +
            "\"date\":${quote(l.date)}," +
            "\"location\":${quote(l.location)}," +
            "\"time\":${quote(l.time)}}"
    }

    private fun setlistFromJson(m: Map<*, *>): Setlist = Setlist(
        id = (m["id"] as? String) ?: "",
        name = (m["name"] as? String) ?: "",
        date = (m["date"] as? String) ?: "",
        location = (m["location"] as? String) ?: "",
        time = (m["time"] as? String) ?: ""
    )

    private fun quote(value: String): String {
        val sb = StringBuilder("\"")
        value.forEach { c ->
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}

private class JsonParser(private val input: String) {
    private var pos = 0

    fun parseObject(): Map<String, Any?>? {
        skipWhitespace()
        val result = mutableMapOf<String, Any?>()
        if (!consume('{')) return null
        skipWhitespace()
        if (consume('}')) return result
        while (true) {
            skipWhitespace()
            val key = parseString() ?: return null
            skipWhitespace()
            if (!consume(':')) return null
            val value = parseValue() ?: return null
            result[key] = value
            skipWhitespace()
            if (consume('}')) return result
            if (!consume(',')) return null
        }
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        return when {
            peek() == '{' -> parseObject()
            peek() == '[' -> parseArray()
            peek() == '"' -> parseString()
            input.startsWith("true", pos) -> { pos += 4; true }
            input.startsWith("false", pos) -> { pos += 5; false }
            input.startsWith("null", pos) -> { pos += 4; null }
            else -> parseNumber()
        }
    }

    private fun parseArray(): List<Any?>? {
        skipWhitespace()
        val result = mutableListOf<Any?>()
        if (!consume('[')) return null
        skipWhitespace()
        if (consume(']')) return result
        while (true) {
            val value = parseValue() ?: return null
            result.add(value)
            skipWhitespace()
            if (consume(']')) return result
            if (!consume(',')) return null
        }
    }

    private fun parseString(): String? {
        if (!consume('"')) return null
        val sb = StringBuilder()
        while (pos < input.length) {
            val c = input[pos]
            when {
                c == '"' -> { pos++; return sb.toString() }
                c == '\\' -> {
                    pos++
                    if (pos >= input.length) return null
                    when (val esc = input[pos]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 >= input.length) return null
                            val hex = input.substring(pos + 1, pos + 5)
                            val code = hex.toIntOrNull(16) ?: return null
                            sb.append(code.toChar())
                            pos += 4
                        }
                        else -> return null
                    }
                    pos++
                }
                else -> { sb.append(c); pos++ }
            }
        }
        return null
    }

    private fun parseNumber(): Any? {
        val start = pos
        while (pos < input.length && (input[pos].isDigit() || input[pos] in "+-.eE")) pos++
        if (pos == start) return null
        val text = input.substring(start, pos)
        return text.toLongOrNull() ?: text.toDoubleOrNull() ?: return null
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun peek(): Char = if (pos < input.length) input[pos] else '\u0000'

    private fun consume(c: Char): Boolean {
        if (pos < input.length && input[pos] == c) {
            pos++
            return true
        }
        return false
    }
}
