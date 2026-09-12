package com.opensetlist.app.data

import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.JustChordsSet
import com.opensetlist.app.model.Song
import kotlin.random.Random

/**
 * Leitura e escrita do formato de arquivo do app JustChords (.jcarchive).
 *
 * Formato: um arquivo ZIP (método STORE, sem compressão) com um único `data.json`
 * na raiz. A raiz tem `songs`, `identity`, `playlists`, `tags` e `dataVersion`
 * (todas obrigatórias para o JustChords importar). Cada música tem `title`,
 * `artist`, `rawData` (corpo em ChordPro), `id` (UUID), `keyChord: {key, minor}`,
 * `date` (epoch seconds) e, opcionalmente, `timeSignature`/`duration`.
 * `playlists` descreve o setlist e referencia as músicas pelos seus `id`.
 *
 * @author ruanitto
 */
object JcArchive {

    const val FILE_EXTENSION = "jcarchive"
    const val MIME_TYPE = "application/zip"

    /**
     * Interpreta o conteúdo de um arquivo .jcarchive, usando o nome do arquivo
     * (sem extensão) como nome do setlist.
     */
    fun parse(fileName: String, bytes: ByteArray): JustChordsSet {
        val root = readZipDataJson(bytes)?.let { JsonParser(it).parseObject() }
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
     * na ordem das músicas. Quando `setlistName` é informado, a saída inclui
     * a playlist referenciando as músicas (import como setlist).
     */
    fun build(songs: List<Song>, setlistName: String? = null): ByteArray =
        ZipData.buildDataJsonZip(buildSongsJson(songs, setlistName))

    /**
     * Serializa as músicas no JSON que o JustChords espera dentro do arquivo,
     * incluindo `identity`, `playlists`, `tags` e `dataVersion` (obrigatórios).
     */
    fun buildSongsJson(songs: List<Song>, setlistName: String? = null): String {
        val date = currentEpochMillis() / 1000
        val songIds = List(songs.size) { randomUuid() }
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
            sb.append("{\"rawData\":").append(jsonQuote(song.body))
                .append(",\"title\":").append(jsonQuote(song.title))
                .append(",\"artist\":").append(jsonQuote(song.artist))
                .append(",\"id\":").append(jsonQuote(songIds[index]))
                .append(",\"keyChord\":{\"key\":").append(jsonQuote(keyBase))
                .append(",\"minor\":").append(minor)
                .append("},\"date\":").append(date)
            if (song.time.isNotBlank()) {
                sb.append(",\"timeSignature\":").append(jsonQuote(song.time))
            }
            if (song.duration.isNotBlank()) {
                sb.append(",\"duration\":").append(jsonQuote(song.duration))
            }
            sb.append('}')
        }
        sb.append("],\"identity\":").append(jsonQuote(randomUuid()))
            .append(",\"playlists\":")
        if (songs.isNotEmpty() && setlistName != null) {
            sb.append("[{\"arrangement\":[")
            songIds.forEachIndexed { index, id ->
                if (index > 0) sb.append(',')
                sb.append("{\"index\":").append(jsonQuote(randomUuid()))
                    .append(",\"notesAndDrawing\":[],\"id\":").append(jsonQuote(id))
                    .append(",\"arrangement\":[],\"type\":\"song\"}")
            }
            sb.append("],\"separateSongSettings\":false,\"title\":")
                .append(jsonQuote(setlistName))
                .append(",\"notes\":\"\",\"eventDate\":null,\"id\":")
                .append(jsonQuote(randomUuid()))
                .append(",\"shared\":false,\"date\":").append(date)
                .append(",\"hasImage\":false,\"deletedEntities\":null,\"lastPushDate\":null}]")
        } else {
            sb.append("[]")
        }
        return sb.append(",\"tags\":[],\"dataVersion\":").append(jsonQuote(randomUuid()))
            .append('}').toString()
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
}