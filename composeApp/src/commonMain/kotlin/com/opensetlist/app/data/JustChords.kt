package com.opensetlist.app.data

import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.JustChordsSet
import com.opensetlist.app.model.Song

/**
 * Leitura e escrita do formato de setlist do app JustChords (.chopro).
 *
 * Formato: o nome do arquivo representa o nome do setlist e cada diretiva
 * `{new_song}` inicia uma nova música na sequência do setlist. O restante é
 * ChordPro padrão (`{title:}`, `{artist:}`, `{key:}`, `{duration:N}` em segundos,
 * corpo com cifras `[G]`/texto).
 *
 * @author ruanitto
 */
object JustChords {

    const val FILE_EXTENSION = "chopro"

    /**
     * Interpreta o conteúdo de um arquivo .chopro, usando o nome do arquivo
     * (sem extensão) como nome do setlist.
     */
    fun parse(fileName: String, content: String): JustChordsSet {
        val blocks = splitSongs(content)
        val songs = blocks.mapNotNull { block ->
            val body = cleanBody(block)
            if (body.isEmpty()) return@mapNotNull null
            val parsed = ChordProParser.parse(body)
            Song(
                id = 0L,
                title = parsed.title.ifBlank { AppStrings.untitledSong },
                artist = parsed.artist.ifBlank { AppStrings.unknownArtist },
                key = parsed.key,
                tempo = parsed.tempo,
                capo = parsed.capo,
                duration = toClockDuration(parsed.duration),
                time = parsed.time,
                youtubeUrl = parsed.youtube,
                body = body,
                transpose = parsed.transpose
            )
        }
        val name = fileName.substringBeforeLast('.', fileName).ifBlank { AppStrings.importedSetlistName }
        return JustChordsSet(name = name, songs = songs)
    }

    /**
     * Monta o conteúdo .chopro de uma setlist, na ordem das músicas.
     */
    fun build(songs: List<Song>): String = buildString {
        songs.forEach { song ->
            appendLine("{new_song}")
            if (song.title.isNotBlank()) appendLine("{title:${song.title}}")
            if (song.artist.isNotBlank() && song.artist != AppStrings.unknownArtist) {
                appendLine("{artist:${song.artist}}")
            }
            if (song.key.isNotBlank()) appendLine("{key:${song.key}}")
            if (song.tempo.isNotBlank()) appendLine("{tempo:${song.tempo}}")
            if (song.capo.isNotBlank()) appendLine("{capo:${song.capo}}")
            if (song.time.isNotBlank()) appendLine("{time:${song.time}}")
            if (song.transpose != 0) appendLine("{transpose:${song.transpose}}")
            if (song.duration.isNotBlank()) {
                val seconds = parseDurationSeconds(song.duration)
                if (seconds > 0) appendLine("{duration:$seconds}")
                else appendLine("{duration:${song.duration}}")
            }
            if (song.body.isNotBlank()) {
                appendLine()
                append(song.body.trim())
                appendLine()
            }
            appendLine()
        }
    }

    /** Divide o conteúdo em blocos, um por música, usando `{new_song}` como separador. */
    private fun splitSongs(content: String): List<String> {
        val blocks = mutableListOf<String>()
        val current = StringBuilder()
        var hasBlock = false
        for (line in content.lines()) {
            if (isNewSongDirective(line)) {
                if (hasBlock) blocks.add(current.toString())
                current.clear()
                hasBlock = true
                continue
            }
            current.appendLine(line)
        }
        if (hasBlock) blocks.add(current.toString())
        else if (current.isNotBlank()) blocks.add(current.toString())
        return blocks
    }

    private fun isNewSongDirective(line: String): Boolean {
        val trimmed = line.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return false
        val inner = trimmed.removeSurrounding("{", "}")
        val name = inner.substringBefore(':').substringBefore(' ').trim()
        return name.equals("new_song", ignoreCase = true)
    }

    /** Remove a diretiva `{scrollspeed:N}` que o JustChords grava no fim de cada música. */
    private fun cleanBody(body: String): String =
        body.lines()
            .filterNot { line ->
                val trimmed = line.trim()
                trimmed.startsWith("{") && trimmed.endsWith("}") &&
                    trimmed.removeSurrounding("{", "}")
                        .substringBefore(':').trim()
                        .equals("scrollspeed", ignoreCase = true)
            }
            .joinToString("\n")
            .trim()

    /** O JustChords grava `{duration:N}` em segundos; converte para o relógio do app ("1:30"). */
    private fun toClockDuration(raw: String): String {
        if (raw.isBlank()) return ""
        if (raw.all { it.isDigit() }) return formatSecondsClock(raw.toLong())
        return raw
    }
}
