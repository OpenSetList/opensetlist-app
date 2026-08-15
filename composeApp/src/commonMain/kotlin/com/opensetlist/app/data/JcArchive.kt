package com.opensetlist.app.data

import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.JustChordsSet
import com.opensetlist.app.model.Song
import kotlin.random.Random

/**
 * Leitura e escrita do formato de arquivo do app JustChords (.jcarchive).
 *
 * Formato: um arquivo ZIP (método STORE, sem compressão) com um único `data.json`
 * na raiz, cujo conteúdo é `{"songs":[...]}`. Cada música tem `title`, `artist`,
 * `duration` (ex.: "4:00"), `timeSignature` (ex.: "4/4"), `keyChord: {key, minor}`,
 * `rawData` (corpo em ChordPro) etc.
 *
 * @author ruanitto
 */
object JcArchive {

    const val FILE_EXTENSION = "jcarchive"
    const val MIME_TYPE = "application/zip"

    internal const val ENTRY_NAME = "data.json"

    /**
     * Interpreta o conteúdo de um arquivo .jcarchive, usando o nome do arquivo
     * (sem extensão) como nome do setlist.
     */
    fun parse(fileName: String, bytes: ByteArray): JustChordsSet {
        val root = readJcArchiveDataJson(bytes)?.let { JsonParser(it).parseObject() }
        val songs = mutableListOf<Song>()
        (root?.get("songs") as? List<*>)?.forEach { raw ->
            val map = raw as? Map<*, *> ?: return@forEach
            val title = (map["title"] as? String)?.trim().orEmpty()
            val body = (map["rawData"] as? String)?.trim().orEmpty()
            if (title.isEmpty() || body.isEmpty()) return@forEach
            val keyChord = map["keyChord"] as? Map<*, *>
            val keyBase = (keyChord?.get("key") as? String)?.trim().orEmpty()
            val minor = keyChord?.get("minor") == true
            songs.add(
                Song(
                    id = 0L,
                    title = title,
                    artist = (map["artist"] as? String)?.trim().orEmpty()
                        .ifBlank { AppStrings.unknownArtist },
                    key = when {
                        keyBase.isEmpty() -> ""
                        minor -> "${keyBase}m"
                        else -> keyBase
                    },
                    tempo = (map["tempo"] as? String) ?: "",
                    duration = (map["duration"] as? String) ?: "",
                    time = (map["timeSignature"] as? String) ?: "",
                    body = JustChords.cleanBody(body)
                )
            )
        }
        val name = fileName.substringBeforeLast('.', fileName)
            .ifBlank { AppStrings.importedSetlistName }
        return JustChordsSet(name = name, songs = songs)
    }

    /**
     * Monta o conteúdo .jcarchive (ZIP com `data.json`) de uma setlist,
     * na ordem das músicas.
     */
    fun build(songs: List<Song>): ByteArray =
        buildDataJsonZip(buildSongsJson(songs))

    /**
     * Serializa as músicas no JSON que o JustChords espera dentro do arquivo.
     */
    fun buildSongsJson(songs: List<Song>): String {
        val date = currentEpochMillis() / 1000.0
        val sb = StringBuilder("{\"songs\":[")
        songs.forEachIndexed { index, song ->
            if (index > 0) sb.append(',')
            val key = song.key.trim()
            val keyBase = if (key.endsWith("m") && key.length > 1) {
                key.dropLast(1)
            } else {
                key
            }
            val minor = key.endsWith("m") && key.length > 1
            sb.append("{\"subtitle\":\"\"")
                .append(",\"date\":").append(date)
                .append(",\"timeSignature\":").append(jsonQuote(song.time))
                .append(",\"tempo\":").append(jsonQuote(song.tempo))
                .append(",\"artist\":").append(jsonQuote(song.artist))
                .append(",\"title\":").append(jsonQuote(song.title))
                .append(",\"duration\":").append(jsonQuote(song.duration))
                .append(",\"id\":").append(jsonQuote(randomUuid()))
                .append(",\"keyChord\":{\"key\":").append(jsonQuote(keyBase))
                .append(",\"minor\":").append(minor)
                .append("},\"rawData\":").append(jsonQuote(song.body))
                .append('}')
        }
        return sb.append("]}").toString()
    }

    /**
     * Gera um ZIP (método STORE, sem compressão) contendo `data.json` na raiz.
     */
    fun buildDataJsonZip(dataJson: String): ByteArray {
        val name = ENTRY_NAME.encodeToByteArray()
        val content = dataJson.encodeToByteArray()
        val crc = crc32(content)
        val size = content.size
        val out = ByteArraySink()
        out.writeInt(0x04034b50)
        out.writeShort(20)
        out.writeShort(0x0800)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeInt(crc)
        out.writeInt(size)
        out.writeInt(size)
        out.writeShort(name.size)
        out.writeShort(0)
        out.write(name)
        out.write(content)
        val centralStart = out.size
        out.writeInt(0x02014b50)
        out.writeShort(20)
        out.writeShort(20)
        out.writeShort(0x0800)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeInt(crc)
        out.writeInt(size)
        out.writeInt(size)
        out.writeShort(name.size)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeInt(0)
        out.writeInt(0)
        out.write(name)
        val centralEnd = out.size
        out.writeInt(0x06054b50)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(1)
        out.writeShort(1)
        out.writeInt(centralEnd - centralStart)
        out.writeInt(centralStart)
        out.writeShort(0)
        return out.toByteArray()
    }

    private fun crc32(bytes: ByteArray): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (byte in bytes) {
            crc = crc xor (byte.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
            }
        }
        return crc.inv()
    }

    private fun randomUuid(): String {
        val hex = Random.nextBytes(16).joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        }.uppercase()
        return hex.substring(0, 8) + "-" +
            hex.substring(8, 12) + "-" +
            hex.substring(12, 16) + "-" +
            hex.substring(16, 20) + "-" +
            hex.substring(20)
    }

    private fun jsonQuote(value: String): String {
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
        return sb.append('"').toString()
    }

    /** Buffer de bytes com escrita little-endian, para montar o ZIP. */
    private class ByteArraySink(initialCapacity: Int = 256) {
        private var bytes = ByteArray(initialCapacity)
        var size = 0
            private set

        val capacity: Int get() = bytes.size

        fun write(value: Int) {
            ensure(1)
            bytes[size++] = (value and 0xFF).toByte()
        }

        fun writeShort(value: Int) {
            write(value)
            write(value ushr 8)
        }

        fun writeInt(value: Int) {
            write(value)
            write(value ushr 8)
            write(value ushr 16)
            write(value ushr 24)
        }

        fun write(src: ByteArray) {
            ensure(src.size)
            src.copyInto(bytes, destinationOffset = size)
            size += src.size
        }

        fun toByteArray(): ByteArray = bytes.copyOf(size)

        private fun ensure(extra: Int) {
            if (size + extra > bytes.size) {
                bytes = bytes.copyOf(maxOf(bytes.size * 2, size + extra))
            }
        }
    }
}

/**
 * Lê o `data.json` de dentro de um arquivo .jcarchive por plataforma.
 */
expect fun readJcArchiveDataJson(bytes: ByteArray): String?
