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

/**
 * Ações de backup no desktop (importa/exporta o banco SQLite via JFileChooser).
 *
 * @author ruanitto
 */
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

private fun columnExists(conn: Connection, table: String, column: String): Boolean {
    conn.createStatement().use { st ->
        st.executeQuery("PRAGMA table_info($table)").use { rs ->
            while (rs.next()) {
                if (rs.getString("name") == column) return true
            }
        }
    }
    return false
}

private fun columnType(conn: Connection, table: String, column: String): String? {
    conn.createStatement().use { st ->
        st.executeQuery("PRAGMA table_info($table)").use { rs ->
            while (rs.next()) {
                if (rs.getString("name") == column) {
                    return rs.getString("type")?.uppercase()
                }
            }
        }
    }
    return null
}

private fun legacySetlistDate(date: String, time: String): Long {
    val dateMillis = toDatePickerMillis(date) ?: return 0L
    val clock = parseClockTime(time)
    if (clock == null) return dateMillis
    return combineDateAndTime(dateMillis, clock.first, clock.second)
}

private fun parseAppDatabase(file: File): BackupData? {
    return runCatching {
        var result: BackupData? = null
        withSqlite(file) { conn ->
            if (!tableExists(conn, "song")) return@withSqlite
            val artists = mutableListOf<Artist>()
            if (tableExists(conn, "artist")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT id, name, creation_date, last_edit FROM artist").use { rs ->
                        while (rs.next()) {
                            artists.add(
                                Artist(
                                    id = rs.getLong(1),
                                    name = rs.getString(2),
                                    creationDate = rs.getLong(3).takeIf { !rs.wasNull() } ?: 0L,
                                    lastEdit = rs.getLong(4).takeIf { !rs.wasNull() } ?: 0L
                                )
                            )
                        }
                    }
                }
            }
            val tags = mutableListOf<Tag>()
            if (tableExists(conn, "tag")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT id, name, creation_date, last_edit FROM tag").use { rs ->
                        while (rs.next()) {
                            tags.add(
                                Tag(
                                    id = rs.getLong(1),
                                    name = rs.getString(2),
                                    creationDate = rs.getLong(3).takeIf { !rs.wasNull() } ?: 0L,
                                    lastEdit = rs.getLong(4).takeIf { !rs.wasNull() } ?: 0L
                                )
                            )
                        }
                    }
                }
            }
            val songTags = mutableMapOf<Long, MutableList<Long>>()
            if (tableExists(conn, "song_tag")) {
                conn.createStatement().use { st ->
                    st.executeQuery("SELECT song_id, tag_id FROM song_tag").use { rs ->
                        while (rs.next()) {
                            songTags.getOrPut(rs.getLong(1)) { mutableListOf() }.add(rs.getLong(2))
                        }
                    }
                }
            }
            val songs = mutableListOf<Song>()
            conn.createStatement().use { st ->
                val hasSongTime = columnExists(conn, "song", "time")
                val hasSongTimestamps = columnExists(conn, "song", "creation_date")
                val hasSongTranspose = columnExists(conn, "song", "transpose")
                st.executeQuery(
                    "SELECT id, title, artist, key, tempo, capo, duration" +
                        (if (hasSongTime) ", time" else "") +
                        ", body, youtube_url" +
                        (if (hasSongTimestamps) ", creation_date, last_edit" else "") +
                        (if (hasSongTranspose) ", transpose" else "") +
                        " FROM song"
                ).use { rs ->
                    while (rs.next()) {
                        songs.add(
                            Song(
                                id = rs.getLong("id"),
                                title = rs.getString("title") ?: "",
                                artist = rs.getString("artist") ?: "",
                                key = rs.getString("key") ?: "",
                                tempo = rs.getString("tempo") ?: "",
                                capo = rs.getString("capo") ?: "",
                                duration = rs.getString("duration") ?: "",
                                time = if (hasSongTime) rs.getString("time") ?: "" else "",
                                body = rs.getString("body") ?: "",
                                youtubeUrl = rs.getString("youtube_url") ?: "",
                                creationDate = rs.getLong("creation_date").takeIf { !rs.wasNull() } ?: 0L,
                                lastEdit = rs.getLong("last_edit").takeIf { !rs.wasNull() } ?: 0L,
                                transpose = if (hasSongTranspose) rs.getLong("transpose").toInt() else 0
                            )
                        )
                    }
                }
            }
            val setlists = mutableListOf<Setlist>()
            val links = mutableListOf<SetlistSongLink>()
            if (tableExists(conn, "setlist")) {
                conn.createStatement().use { st ->
                    val dateIsInteger = columnType(conn, "setlist", "date") == "INTEGER"
                    val hasSetlistTime = columnExists(conn, "setlist", "time")
                    st.executeQuery(
                        "SELECT id, name, date, location" +
                            (if (hasSetlistTime) ", time" else "") +
                            ", creation_date, last_edit FROM setlist"
                    ).use { rs ->
                        while (rs.next()) {
                            val date = if (dateIsInteger) {
                                rs.getLong("date").takeIf { !rs.wasNull() } ?: 0L
                            } else {
                                legacySetlistDate(
                                    date = rs.getString("date") ?: "",
                                    time = if (hasSetlistTime) rs.getString("time") ?: "" else ""
                                )
                            }
                            setlists.add(
                                Setlist(
                                    id = rs.getLong(1),
                                    name = rs.getString(2),
                                    date = date,
                                    location = rs.getString(4),
                                    creationDate = rs.getLong(6).takeIf { !rs.wasNull() } ?: 0L,
                                    lastEdit = rs.getLong(7).takeIf { !rs.wasNull() } ?: 0L
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
                                    setlistId = rs.getLong(1),
                                    songId = rs.getLong(2),
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
