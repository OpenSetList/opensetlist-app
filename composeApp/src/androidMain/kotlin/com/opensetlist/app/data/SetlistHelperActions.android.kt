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
import com.opensetlist.app.model.HelperSetlist
import com.opensetlist.app.model.SetlistHelperBackup
import com.opensetlist.app.model.Song
import java.io.File

/**
 * Importação de backup do SetList Helper no Android (banco SQLite).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberSetlistHelperActions(
    onImported: (SetlistHelperBackup?) -> Unit
): SetlistHelperActions {
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = runCatching {
                val cacheFile = File(context.cacheDir, "slh_import.db")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                parseSetlistHelperDb(cacheFile)
            }.getOrNull()
            onImported(result)
        }
    }

    return remember {
        SetlistHelperActions(
            importBackup = {
                importLauncher.launch(
                    arrayOf(
                        "application/octet-stream",
                        "application/x-sqlite3",
                        "application/vnd.sqlite3"
                    )
                )
            }
        )
    }
}

private fun parseSetlistHelperDb(file: File): SetlistHelperBackup? {
    val db = try {
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    } catch (e: Exception) {
        return null
    }

    try {
        if (!tableExists(db, "songs")) return null

        val artists = mutableMapOf<Long, String>()
        if (tableExists(db, "ARTIST")) {
            db.rawQuery("SELECT _id, name FROM ARTIST", null).use { cursor ->
                while (cursor.moveToNext()) {
                    artists[cursor.getLongByName("_id")] = cursor.getStringByName("name")
                }
            }
        }

        val genres = mutableMapOf<Long, String>()
        if (tableExists(db, "Genre")) {
            db.rawQuery("SELECT _id, name FROM Genre", null).use { cursor ->
                while (cursor.moveToNext()) {
                    genres[cursor.getLongByName("_id")] = cursor.getStringByName("name")
                }
            }
        }

        val tagNamesById = mutableMapOf<Long, String>()
        if (tableExists(db, "Tag")) {
            db.rawQuery("SELECT _id, name FROM Tag", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tagNamesById[cursor.getLongByName("_id")] = cursor.getStringByName("name")
                }
            }
        }

        val songTagNames = mutableMapOf<Long, MutableList<String>>()
        if (tableExists(db, "TagSongs")) {
            db.rawQuery("SELECT song_id, tag_id FROM TagSongs", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val songId = cursor.getLongByName("song_id")
                    val tagName = cursor.getLongOrNullByName("tag_id")?.let { tagNamesById[it] }
                    if (tagName != null) {
                        songTagNames.getOrPut(songId) { mutableListOf() }.add(tagName)
                    }
                }
            }
        }

        val songs = mutableListOf<Song>()
        db.rawQuery(
            "SELECT _id, name, song_key, tempo, artist_id, genre_id, youtube_url, " +
                "time_signature_top, time_signature_bottom, song_length, notes, other, lyrics, " +
                "creation_date, last_edit " +
                "FROM songs " +
                "WHERE deleted = 0 OR deleted IS NULL", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLongByName("_id")
                val title = cursor.getStringByName("name").ifBlank { "Música sem título" }
                val artist = cursor.getLongOrNullByName("artist_id")?.let { artists[it] }
                    ?: "Artista desconhecido"
                val key = cursor.getStringOrNullByName("song_key") ?: ""
                val tempo = cursor.getLongOrNullByName("tempo")
                    ?.let { if (it > 0) it.toString() else "" } ?: ""
                val time = SetlistHelperMapping.buildTimeSignature(
                    cursor.getLongOrNullByName("time_signature_top"),
                    cursor.getLongOrNullByName("time_signature_bottom")
                )
                val duration = cursor.getLongOrNullByName("song_length")
                    ?.takeIf { it > 0 }
                    ?.let { formatSecondsClock(it) } ?: ""
                val youtube = cursor.getStringOrNullByName("youtube_url") ?: ""
                val lyrics = cursor.getStringOrNullByName("lyrics") ?: ""
                val body = SetlistHelperMapping.buildImportBody(
                    cursor.getStringOrNullByName("notes") ?: "",
                    cursor.getStringOrNullByName("other") ?: "",
                    lyrics
                )
                // COMMENT: Not set gender as tag, because it is not a user-defined tag, but a property of the song in Setlist Helper. It can be used to filter songs, but it is not a tag that the user can manage.
                // TODO: Create a x_gender directive, table, and edit option with autocomplete suggestion. 
                // val genreName = cursor.getLongOrNullByName("genre_id")?.let { genres[it] }
                // if (genreName != null) {
                //     songTagNames.getOrPut(id) { mutableListOf() }.add(genreName)
                // }
                val now = System.currentTimeMillis()
                val creation = normalizeEpoch(
                    cursor.getLongOrNullByName("creation_date"),
                    now
                )
                val lastEdit = normalizeEpoch(
                    cursor.getLongOrNullByName("last_edit"),
                    now
                ).coerceAtLeast(creation)
                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        key = key,
                        tempo = tempo,
                        capo = "",
                        duration = duration,
                        time = time,
                        youtubeUrl = youtube,
                        body = body,
                        creationDate = creation,
                        lastEdit = lastEdit
                    )
                )
            }
        }

        val songTags = songTagNames.mapKeys { it.key }.mapValues { it.value.distinct() }

        val importedSongIds = songs.map { it.id }.toSet()

        val setlists = mutableListOf<HelperSetlist>()
        if (tableExists(db, "setlist") && tableExists(db, "setlistsong")) {
            val songIdBySetlist = mutableMapOf<Long, MutableList<Long>>()
            db.rawQuery(
                "SELECT songid, setlistid, displaysequencenumber FROM setlistsong " +
                    "ORDER BY setlistid ASC, displaysequencenumber ASC", null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val setId = cursor.getLongByName("setlistid")
                    val songId = cursor.getLongByName("songid")
                    if (songId in importedSongIds) {
                        songIdBySetlist.getOrPut(setId) { mutableListOf() }.add(songId)
                    }
                }
            }

            db.rawQuery(
                "SELECT _id, name, gig_location, gig_date FROM setlist " +
                    "WHERE deleted = 0 OR deleted IS NULL", null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val setId = cursor.getLongByName("_id")
                    val name = cursor.getStringByName("name").ifBlank { "Setlist importada" }
                    val location = cursor.getStringOrNullByName("gig_location") ?: ""
                    val date = cursor.getLongOrNullByName("gig_date")?.takeIf { it > 0 } ?: 0L
                    setlists.add(
                        HelperSetlist(
                            name = name,
                            date = date,
                            location = location,
                            songIds = songIdBySetlist[setId] ?: emptyList()
                        )
                    )
                }
            }
        }

        return SetlistHelperBackup(songs, setlists, songTags)
    } finally {
        db.close()
    }
}

private fun normalizeEpoch(value: Long?, fallback: Long): Long {
    if (value == null || value <= 0) return fallback
    return value
}

private fun tableExists(db: SQLiteDatabase, table: String): Boolean {
    db.rawQuery(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        arrayOf(table)
    ).use { cursor -> return cursor.moveToNext() }
}

private fun Cursor.getLongByName(column: String): Long = getLong(getColumnIndexOrThrow(column))

private fun Cursor.getStringByName(column: String): String = getString(getColumnIndexOrThrow(column))

private fun Cursor.getLongOrNullByName(column: String): Long? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) return null
    return getLong(index)
}

private fun Cursor.getStringOrNullByName(column: String): String? {
    val index = getColumnIndex(column)
    if (index < 0 || isNull(index)) return null
    return getString(index)
}
