package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.opensetlist.app.model.HelperSetlist
import com.opensetlist.app.model.SetlistHelperBackup
import com.opensetlist.app.model.Song
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.JFileChooser

/**
 * Importação de backup do SetList Helper no desktop (banco SQLite).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberSetlistHelperActions(
    onImported: (SetlistHelperBackup?) -> Unit
): SetlistHelperActions {
    return remember {
        SetlistHelperActions(
            importBackup = {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Importar backup do SetList Helper"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val result = runCatching {
                        parseSetlistHelperDb(chooser.selectedFile)
                    }.getOrNull()
                    onImported(result)
                }
            }
        )
    }
}

private fun parseSetlistHelperDb(file: File): SetlistHelperBackup? {
    return runCatching {
        var result: SetlistHelperBackup? = null
        withSqlite(file) { conn ->
            if (!tableExists(conn, "songs")) return@withSqlite

            val artists = mutableMapOf<Long, String>()
            if (tableExists(conn, "ARTIST")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT _id, name FROM ARTIST").use { rs ->
                        while (rs.next()) {
                            artists[rs.getLong(1)] = rs.getString(2)
                        }
                    }
                }
            }

            val genres = mutableMapOf<Long, String>()
            if (tableExists(conn, "Genre")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT _id, name FROM Genre").use { rs ->
                        while (rs.next()) {
                            genres[rs.getLong(1)] = rs.getString(2)
                        }
                    }
                }
            }

            val tagNamesById = mutableMapOf<Long, String>()
            if (tableExists(conn, "Tag")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT _id, name FROM Tag").use { rs ->
                        while (rs.next()) {
                            tagNamesById[rs.getLong(1)] = rs.getString(2)
                        }
                    }
                }
            }

            val songTagNames = mutableMapOf<Long, MutableList<String>>()
            if (tableExists(conn, "TagSongs")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT song_id, tag_id FROM TagSongs").use { rs ->
                        while (rs.next()) {
                            val songId = rs.getLong(1)
                            val tagId = rs.getLong(2)
                            val tagName = if (rs.wasNull()) null else tagNamesById[tagId]
                            if (tagName != null) {
                                songTagNames.getOrPut(songId) { mutableListOf() }.add(tagName)
                            }
                        }
                    }
                }
            }

            val songs = mutableListOf<Song>()
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT _id, name, song_key, tempo, artist_id, genre_id, youtube_url, " +
                        "time_signature_top, time_signature_bottom, song_length, notes, other, lyrics " +
                        "FROM songs " +
                        "WHERE deleted = 0 OR deleted IS NULL"
                ).use { rs ->
                    while (rs.next()) {
                        val id = rs.getLong(1).toString()
                        val title = rs.getString(2)?.ifBlank { "Música sem título" }
                            ?: "Música sem título"
                        val artistId = rs.getLong(5)
                        val artist = if (rs.wasNull()) "Artista desconhecido"
                        else artists[artistId] ?: "Artista desconhecido"
                        val time = SetlistHelperMapping.buildTimeSignature(
                            rs.getLong(8).takeIf { !rs.wasNull() },
                            rs.getLong(9).takeIf { !rs.wasNull() }
                        )
                        val songLength = rs.getLong(10)
                        val duration = if (rs.wasNull() || songLength <= 0) "" else
                            formatSecondsClock(songLength)
                        val body = SetlistHelperMapping.buildImportBody(
                            rs.getString(11) ?: "",
                            rs.getString(12) ?: "",
                            rs.getString(13) ?: ""
                        )
                        val genreId = rs.getLong(6)
                        val genreName = if (rs.wasNull()) null else genres[genreId]
                        if (genreName != null) {
                            songTagNames.getOrPut(rs.getLong(1)) { mutableListOf() }.add(genreName)
                        }
                        songs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                key = rs.getString(3) ?: "",
                                tempo = if (rs.getLong(4) > 0) rs.getLong(4).toString() else "",
                                capo = "",
                                duration = duration,
                                time = time,
                                youtubeUrl = rs.getString(7) ?: "",
                                body = body
                            )
                        )
                    }
                }
            }

            val songTags = songTagNames.mapKeys { it.key.toString() }.mapValues { it.value.distinct() }

            val importedSongIds = songs.map { it.id }.toSet()
            val setlists = mutableListOf<HelperSetlist>()
            if (tableExists(conn, "setlist") && tableExists(conn, "setlistsong")) {
                val songIdBySetlist = mutableMapOf<Long, MutableList<String>>()
                conn.createStatement().use { st ->
                    st.executeQuery(
                        "SELECT songid, setlistid, displaysequencenumber FROM setlistsong " +
                            "ORDER BY setlistid ASC, displaysequencenumber ASC"
                    ).use { rs ->
                        while (rs.next()) {
                            val setId = rs.getLong(2)
                            val songId = rs.getLong(1).toString()
                            if (songId in importedSongIds) {
                                songIdBySetlist.getOrPut(setId) { mutableListOf() }.add(songId)
                            }
                        }
                    }
                }
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                conn.createStatement().use { st ->
                    st.executeQuery(
                        "SELECT _id, name, gig_location, gig_date FROM setlist " +
                            "WHERE deleted = 0 OR deleted IS NULL"
                    ).use { rs ->
                        while (rs.next()) {
                            val setId = rs.getLong(1)
                            val dateMillis = rs.getLong(4)
                            val date = if (rs.wasNull() || dateMillis <= 0) "" else
                                dateFormat.format(Date(dateMillis))
                            setlists.add(
                                HelperSetlist(
                                    name = rs.getString(2)?.ifBlank { "Setlist importada" }
                                        ?: "Setlist importada",
                                    date = date,
                                    location = rs.getString(3) ?: "",
                                    songIds = songIdBySetlist[setId] ?: emptyList()
                                )
                            )
                        }
                    }
                }
            }
            result = SetlistHelperBackup(songs, setlists, songTags)
        }
        result
    }.getOrNull()
}
