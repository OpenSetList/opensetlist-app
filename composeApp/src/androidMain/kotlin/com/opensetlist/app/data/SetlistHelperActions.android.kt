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
import java.text.SimpleDateFormat
import java.util.Locale

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

        val songs = mutableListOf<Song>()
        db.rawQuery(
            "SELECT _id, name, song_key, tempo, artist_id, lyrics FROM songs " +
                "WHERE deleted = 0 OR deleted IS NULL", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLongByName("_id").toString()
                val title = cursor.getStringByName("name").ifBlank { "Música sem título" }
                val artist = cursor.getLongOrNullByName("artist_id")?.let { artists[it] }
                    ?: "Artista desconhecido"
                val key = cursor.getStringOrNullByName("song_key") ?: ""
                val tempo = cursor.getLongOrNullByName("tempo")
                    ?.let { if (it > 0) it.toString() else "" } ?: ""
                val lyrics = cursor.getStringOrNullByName("lyrics") ?: ""
                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        key = key,
                        tempo = tempo,
                        capo = "",
                        body = lyrics
                    )
                )
            }
        }

        val importedSongIds = songs.map { it.id }.toSet()

        val setlists = mutableListOf<HelperSetlist>()
        if (tableExists(db, "setlist") && tableExists(db, "setlistsong")) {
            val songIdBySetlist = mutableMapOf<Long, MutableList<String>>()
            db.rawQuery(
                "SELECT songid, setlistid, displaysequencenumber FROM setlistsong " +
                    "ORDER BY setlistid ASC, displaysequencenumber ASC", null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val setId = cursor.getLongByName("setlistid")
                    val songId = cursor.getLongByName("songid").toString()
                    if (songId in importedSongIds) {
                        songIdBySetlist.getOrPut(setId) { mutableListOf() }.add(songId)
                    }
                }
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            db.rawQuery(
                "SELECT _id, name, gig_location, gig_date FROM setlist " +
                    "WHERE deleted = 0 OR deleted IS NULL", null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val setId = cursor.getLongByName("_id")
                    val name = cursor.getStringByName("name").ifBlank { "Setlist importada" }
                    val location = cursor.getStringOrNullByName("gig_location") ?: ""
                    val date = cursor.getLongOrNullByName("gig_date")
                        ?.takeIf { it > 0 }
                        ?.let { runCatching { dateFormat.format(it) }.getOrNull() }
                        ?: ""
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

        return SetlistHelperBackup(songs, setlists)
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
