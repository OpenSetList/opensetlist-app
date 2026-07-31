package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.opensetlist.app.model.Artist
import com.opensetlist.app.model.BackupData
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.SetlistSongLink
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import javax.swing.JFileChooser

@Composable
actual fun rememberBackupActions(
    onImported: (BackupData?) -> Unit
): BackupActions {
    return remember {
        BackupActions(
            importBackup = {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Importar backup"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val result = runCatching {
                        parseAppDatabase(chooser.selectedFile)
                    }.getOrNull()
                    onImported(result)
                }
            },
            exportBytes = {
                runCatching { appDbFile().readBytes() }.getOrNull()
            }
        )
    }
}

internal fun withSqlite(file: File, block: (Connection) -> Unit) {
    DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
        block(conn)
    }
}

internal fun tableExists(conn: Connection, table: String): Boolean {
    conn.prepareStatement(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
    ).use { stmt ->
        stmt.setString(1, table)
        stmt.executeQuery().use { rs -> return rs.next() }
    }
}

private fun parseAppDatabase(file: File): BackupData? {
    return runCatching {
        var result: BackupData? = null
        withSqlite(file) { conn ->
            if (!tableExists(conn, "song")) return@withSqlite
            val artists = mutableListOf<Artist>()
            if (tableExists(conn, "artist")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT id, name FROM artist").use { rs ->
                        while (rs.next()) {
                            artists.add(Artist(id = rs.getString(1), name = rs.getString(2)))
                        }
                    }
                }
            }
            val tags = mutableListOf<Tag>()
            if (tableExists(conn, "tag")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT id, name FROM tag").use { rs ->
                        while (rs.next()) {
                            tags.add(Tag(id = rs.getString(1), name = rs.getString(2)))
                        }
                    }
                }
            }
            val songTags = mutableMapOf<String, MutableList<String>>()
            if (tableExists(conn, "song_tag")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT song_id, tag_id FROM song_tag").use { rs ->
                        while (rs.next()) {
                            songTags.getOrPut(rs.getString(1)) { mutableListOf() }.add(rs.getString(2))
                        }
                    }
                }
            }
            val songs = mutableListOf<Song>()
            conn.createStatement().use { st ->
                st.executeQuery(
                    "SELECT id, title, artist, key, tempo, capo, duration, body, youtube_url FROM song"
                ).use { rs ->
                    while (rs.next()) {
                        songs.add(
                            Song(
                                id = rs.getString(1),
                                title = rs.getString(2),
                                artist = rs.getString(3),
                                key = rs.getString(4),
                                tempo = rs.getString(5),
                                capo = rs.getString(6),
                                duration = rs.getString(7),
                                body = rs.getString(8),
                                youtubeUrl = rs.getString(9)
                            )
                        )
                    }
                }
            }
            val setlists = mutableListOf<Setlist>()
            val links = mutableListOf<SetlistSongLink>()
            if (tableExists(conn, "setlist")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT id, name, date, location, time FROM setlist").use { rs ->
                        while (rs.next()) {
                            setlists.add(
                                Setlist(
                                    id = rs.getString(1),
                                    name = rs.getString(2),
                                    date = rs.getString(3),
                                    location = rs.getString(4),
                                    time = rs.getString(5)
                                )
                            )
                        }
                    }
                }
            }
            if (tableExists(conn, "setlist_song")) {
                conn.createStatement().use { st ->
                    st.executeQuery(
                        "SELECT setlist_id, song_id, position FROM setlist_song " +
                            "ORDER BY setlist_id ASC, position ASC"
                    ).use { rs ->
                        while (rs.next()) {
                            links.add(
                                SetlistSongLink(
                                    setlistId = rs.getString(1),
                                    songId = rs.getString(2),
                                    position = rs.getLong(3).toInt()
                                )
                            )
                        }
                    }
                }
            }
            result = BackupData(
                songs = songs,
                setlists = setlists,
                links = links,
                artists = artists,
                tags = tags,
                songTags = songTags.mapValues { it.value.toList() }
            )
        }
        result
    }.getOrNull()
}
