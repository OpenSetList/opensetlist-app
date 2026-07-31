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

            val songs = mutableListOf<Song>()
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT _id, name, song_key, tempo, artist_id, lyrics FROM songs " +
                        "WHERE deleted = 0 OR deleted IS NULL"
                ).use { rs ->
                    while (rs.next()) {
                        val id = rs.getLong(1).toString()
                        val title = rs.getString(2)?.ifBlank { "Música sem título" }
                            ?: "Música sem título"
                        val artistId = rs.getLong(5)
                        val artist = if (rs.wasNull()) "Artista desconhecido"
                        else artists[artistId] ?: "Artista desconhecido"
                        songs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                key = rs.getString(3) ?: "",
                                tempo = if (rs.getLong(4) > 0) rs.getLong(4).toString() else "",
                                capo = "",
                                body = rs.getString(6) ?: ""
                            )
                        )
                    }
                }
            }

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
            result = SetlistHelperBackup(songs, setlists)
        }
        result
    }.getOrNull()
}
