package com.opensetlist.app.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.opensetlist.app.model.Artist
import com.opensetlist.app.model.BackupData
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.SetlistSongLink
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag
import java.io.File

/**
 * Ações de backup no Android (importa/exporta o banco SQLite via SAF).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberBackupActions(
    onImported: (BackupData?) -> Unit
): BackupActions {
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = runCatching {
                val cacheFile = File(context.cacheDir, "app_backup.db")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                parseAppDatabase(cacheFile)
            }.getOrNull()
            onImported(result)
        }
    }

    return remember {
        BackupActions(
            importBackup = {
                importLauncher.launch(
                    arrayOf(
                        "application/octet-stream",
                        "application/x-sqlite3",
                        "application/vnd.sqlite3",
                        "application/x-sqlite3"
                    )
                )
            },
            exportBytes = {
                runCatching {
                    context.getDatabasePath("setlist.db").readBytes()
                }.getOrNull()
            }
        )
    }
}

private fun parseAppDatabase(file: File): BackupData? {
    val db = try {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    } catch (e: Exception) {
        return null
    }

    try {
        if (!tableExists(db, "song")) return null

        val artists = mutableListOf<Artist>()
        if (tableExists(db, "artist")) {
            db.rawQuery("SELECT id, name, creation_date, last_edit FROM artist", null).use { cursor ->
                while (cursor.moveToNext()) {
                    artists.add(
                        Artist(
                            id = cursor.getLongByName("id"),
                            name = cursor.getStringOrNullByName("name") ?: "",
                            creationDate = cursor.getLongOrNullByName("creation_date") ?: 0L,
                            lastEdit = cursor.getLongOrNullByName("last_edit") ?: 0L
                        )
                    )
                }
            }
        }

        val tags = mutableListOf<Tag>()
        if (tableExists(db, "tag")) {
            db.rawQuery("SELECT id, name, creation_date, last_edit FROM tag", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tags.add(
                        Tag(
                            id = cursor.getLongByName("id"),
                            name = cursor.getStringOrNullByName("name") ?: "",
                            creationDate = cursor.getLongOrNullByName("creation_date") ?: 0L,
                            lastEdit = cursor.getLongOrNullByName("last_edit") ?: 0L
                        )
                    )
                }
            }
        }

        val songTags = mutableMapOf<Long, MutableList<Long>>()
        if (tableExists(db, "song_tag")) {
            db.rawQuery("SELECT song_id, tag_id FROM song_tag", null).use { cursor ->
                while (cursor.moveToNext()) {
                    songTags.getOrPut(cursor.getLongByName("song_id")) { mutableListOf() }
                        .add(cursor.getLongByName("tag_id"))
                }
            }
        }

        val songs = mutableListOf<Song>()
        val hasSongTime = columnExists(db, "song", "time")
        val hasSongTimestamps = columnExists(db, "song", "creation_date")
        db.rawQuery(
            if (hasSongTime) {
                "SELECT id, title, artist, key, tempo, capo, duration, time, body, youtube_url, " +
                    if (hasSongTimestamps) "creation_date, last_edit" else ""
            } else {
                "SELECT id, title, artist, key, tempo, capo, duration, body, youtube_url, " +
                    if (hasSongTimestamps) "creation_date, last_edit" else ""
            },
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(
                    Song(
                        id = cursor.getLongByName("id"),
                        title = cursor.getStringOrNullByName("title") ?: "",
                        artist = cursor.getStringOrNullByName("artist") ?: "",
                        key = cursor.getStringOrNullByName("key") ?: "",
                        tempo = cursor.getStringOrNullByName("tempo") ?: "",
                        capo = cursor.getStringOrNullByName("capo") ?: "",
                        duration = cursor.getStringOrNullByName("duration") ?: "",
                        time = if (hasSongTime) cursor.getStringOrNullByName("time") ?: "" else "",
                        youtubeUrl = cursor.getStringOrNullByName("youtube_url") ?: "",
                        body = cursor.getStringOrNullByName("body") ?: "",
                        creationDate = cursor.getLongOrNullByName("creation_date") ?: 0L,
                        lastEdit = cursor.getLongOrNullByName("last_edit") ?: 0L
                    )
                )
            }
        }

        val setlists = mutableListOf<Setlist>()
        val links = mutableListOf<SetlistSongLink>()
        if (tableExists(db, "setlist")) {
            db.rawQuery(
                "SELECT id, name, date, location, time, creation_date, last_edit FROM setlist",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    setlists.add(
                        Setlist(
                            id = cursor.getLongByName("id"),
                            name = cursor.getStringOrNullByName("name") ?: "",
                            date = cursor.getStringOrNullByName("date") ?: "",
                            location = cursor.getStringOrNullByName("location") ?: "",
                            time = cursor.getStringOrNullByName("time") ?: "",
                            creationDate = cursor.getLongOrNullByName("creation_date") ?: 0L,
                            lastEdit = cursor.getLongOrNullByName("last_edit") ?: 0L
                        )
                    )
                }
            }
        }
        if (tableExists(db, "setlist_song")) {
            db.rawQuery(
                "SELECT setlist_id, song_id, position FROM setlist_song ORDER BY setlist_id ASC, position ASC",
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    links.add(
                        SetlistSongLink(
                            setlistId = cursor.getLongByName("setlist_id"),
                            songId = cursor.getLongByName("song_id"),
                            position = cursor.getLongByName("position").toInt()
                        )
                    )
                }
            }
        }

        return BackupData(
            songs = songs,
            setlists = setlists,
            links = links,
            artists = artists,
            tags = tags,
            songTags = songTags.mapValues { it.value.toList() }
        )
    } finally {
        db.close()
    }
}

private fun tableExists(db: SQLiteDatabase, table: String): Boolean {
    db.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        arrayOf(table)
    ).use { cursor -> return cursor.moveToNext() }
}

private fun columnExists(db: SQLiteDatabase, table: String, column: String): Boolean {
    db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
        while (cursor.moveToNext()) {
            if (cursor.getStringOrNullByName("name") == column) return true
        }
    }
    return false
}

private fun Cursor.getLongByName(column: String): Long = getLong(getColumnIndexOrThrow(column))

private fun Cursor.getLongOrNullByName(column: String): Long? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) return null
    return getLong(index)
}

private fun Cursor.getStringByName(column: String): String = getString(getColumnIndexOrThrow(column))

private fun Cursor.getStringOrNullByName(column: String): String? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) return null
    return getString(index)
}
