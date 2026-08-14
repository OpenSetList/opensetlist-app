package com.opensetlist.app.data

import com.opensetlist.app.AppStrings
import com.opensetlist.app.data.db.AppDatabase
import com.opensetlist.app.data.db.Artist as DbArtist
import com.opensetlist.app.data.db.Setlist as DbSetlist
import com.opensetlist.app.data.db.Song as DbSong
import com.opensetlist.app.data.db.Tag as DbTag
import com.opensetlist.app.model.Artist
import com.opensetlist.app.model.BackupData
import com.opensetlist.app.model.HelperSetlist
import com.opensetlist.app.model.JustChordsSet
import com.opensetlist.app.model.SetShareData
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.SetlistHelperBackup
import com.opensetlist.app.model.SetlistSongLink
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag

/**
 * Repositório único de acesso ao banco SQLDelight: músicas, artistas, tags e setlists.
 *
 * @author ruanitto
 */
class SongRepository(private val database: AppDatabase) {
    private val queries = database.appDatabaseQueries

    fun seedIfEmpty() {
        if (queries.selectAllSongs().executeAsList().isNotEmpty()) return
        val now = System.currentTimeMillis()
        SampleSongs.songs.forEachIndexed { index, song ->
            ensureArtist(song.artist)
            queries.insertSongWithId(
                song.id, song.title, song.artist, song.key, song.tempo, song.capo,
                song.duration.ifBlank { null }, song.time.ifBlank { null }, song.body,
                song.youtubeUrl.ifBlank { null }, index.toLong(), now, now, song.transpose.toLong()
            )
        }
        SampleSongs.allSetlists.forEach { setlist ->
            queries.insertSetlistWithId(
                setlist.id,
                setlist.name,
                setlist.date.takeIf { it > 0 },
                setlist.location.ifBlank { null },
                now,
                now
            )
            setlist.songs.forEachIndexed { pos, song ->
                queries.insertSetlistSong(setlist.id, song.id, pos.toLong())
            }
        }
    }

    fun allSongs(): List<Song> =
        queries.selectAllSongs().executeAsList().map { it.toModel() }

    fun getSong(id: Long): Song? =
        queries.selectSongById(id).executeAsOneOrNull()?.toModel()

    fun upsert(song: Song): Song {
        ensureArtist(song.artist)
        val now = System.currentTimeMillis()
        val existing = if (song.id != 0L) {
            queries.selectSongById(song.id).executeAsOneOrNull()
        } else {
            null
        }
        if (existing != null) {
            queries.updateSongBody(
                body = song.body,
                title = song.title,
                artist = song.artist,
                key = if (song.key.isBlank()) existing.key else song.key,
                tempo = if (song.tempo.isBlank()) existing.tempo else song.tempo,
                capo = if (song.capo.isBlank()) existing.capo else song.capo,
                duration = if (song.duration.isBlank()) existing.duration else song.duration,
                time = if (song.time.isBlank()) existing.time else song.time,
                youtube_url = if (song.youtubeUrl.isBlank()) existing.youtube_url else song.youtubeUrl,
                last_edit = now,
                transpose = song.transpose.toLong(),
                id = existing.id
            )
            return song.copy(
                id = existing.id,
                creationDate = existing.creation_date,
                lastEdit = now
            )
        }
        val creation = maxOf(song.creationDate, song.lastEdit).takeIf { it > 0 } ?: now
        queries.insertSong(
            title = song.title,
            artist = song.artist,
            key = song.key.ifBlank { null },
            tempo = song.tempo.ifBlank { null },
            capo = song.capo.ifBlank { null },
            duration = song.duration.ifBlank { null },
            time = song.time.ifBlank { null },
            body = song.body,
            youtube_url = song.youtubeUrl.ifBlank { null },
            sort_order = queries.selectMaxSortOrder().executeAsOne(),
            creation_date = creation,
            last_edit = creation,
            transpose = song.transpose.toLong()
        )
        val newId = queries.lastInsertRowId().executeAsOne()
        return song.copy(id = newId, creationDate = creation, lastEdit = creation)
    }

    fun delete(id: Long) {
        database.transaction {
            queries.deleteSetlistSongBySong(id)
            queries.deleteSong(id)
        }
    }

    fun allSetlists(): List<Setlist> =
        queries.selectAllSetlists().executeAsList().map { it.toModel() }

    fun songsInSetlist(setlistId: Long): List<Song> =
        queries.selectSongsInSetlist(setlistId).executeAsList().map { it.toModel() }

    fun importSong(body: String): Song {
        val parsed = ChordProParser.parse(body)
        val song = Song(
            id = 0L,
            title = parsed.title.ifBlank { AppStrings.untitledSong },
            artist = parsed.artist.ifBlank { AppStrings.unknownArtist },
            key = parsed.key,
            tempo = parsed.tempo,
            capo = parsed.capo,
            duration = parsed.duration,
            time = parsed.time,
            youtubeUrl = parsed.youtube,
            body = body,
            transpose = parsed.transpose
        )
        var imported: Song? = null
        database.transaction {
            val result = importSongWithDedup(song)
            addTagsToSong(result.id, parsed.tags)
            imported = result
        }
        return imported ?: song
    }

    fun newSong(): Song = Song(
        id = 0L,
        title = AppStrings.newSongTitle,
        artist = AppStrings.defaultArtistName,
        key = "",
        tempo = "",
        capo = "",
        body = ""
    )

    fun createSetlist(name: String): Setlist {
        val now = System.currentTimeMillis()
        queries.insertSetlist(name, null, null, now, now)
        val newId = queries.lastInsertRowId().executeAsOne()
        return Setlist(id = newId, name = name, songs = emptyList(), creationDate = now, lastEdit = now)
    }

    fun renameSetlist(id: Long, name: String) {
        queries.renameSetlist(name, System.currentTimeMillis(), id)
    }

    fun updateSetlistInfo(id: Long, date: Long, location: String) {
        queries.updateSetlistInfo(
            date.takeIf { it > 0 },
            location.ifBlank { null },
            System.currentTimeMillis(),
            id
        )
    }

    fun deleteSetlist(id: Long) {
        database.transaction {
            queries.deleteAllSetlistSongs(id)
            queries.deleteSetlist(id)
        }
    }

    fun addSongToSetlist(setlistId: Long, songId: Long) {
        val position = queries.nextPositionInSetlist(setlistId).executeAsOne()
        queries.insertSetlistSong(setlistId, songId, position)
    }

    fun removeSongFromSetlist(setlistId: Long, songId: Long) {
        queries.deleteSetlistSong(setlistId, songId)
    }

    fun reorderSetlistSongs(setlistId: Long, orderedSongIds: List<Long>) {
        database.transaction {
            queries.deleteAllSetlistSongs(setlistId)
            orderedSongIds.forEachIndexed { index, songId ->
                queries.insertSetlistSong(setlistId, songId, index.toLong())
            }
        }
    }

    fun songsNotInSetlist(setlistId: Long): List<Song> =
        queries.selectSongsNotInSetlist(setlistId).executeAsList().map { it.toModel() }

    fun backupData(): BackupData {
        val links = queries.selectAllLinks().executeAsList().map {
            SetlistSongLink(it.setlist_id, it.song_id, it.position.toInt())
        }
        return BackupData(allSongs(), allSetlists(), links)
    }

    fun restoreBackup(data: BackupData): Boolean {
        if (data.songs.isEmpty() && data.setlists.isEmpty()) return false
        val now = System.currentTimeMillis()
        val songIdMap = mutableMapOf<Long, Long>()
        val tagIdMap = mutableMapOf<Long, Long>()
        val setlistIdMap = mutableMapOf<Long, Long>()
        database.transaction {
            queries.deleteAllSongTags()
            queries.deleteAllLinks()
            queries.deleteAllSetlists()
            queries.deleteAllSongs()
            queries.deleteAllArtists()
            data.artists.forEach { artist ->
                val creation = artist.creationDate.takeIf { it > 0 } ?: now
                val lastEdit = artist.lastEdit.takeIf { it > 0 } ?: creation
                queries.insertArtist(
                    name = artist.name,
                    creation_date = creation,
                    last_edit = lastEdit
                )
            }
            data.tags.forEach { tag ->
                val creation = tag.creationDate.takeIf { it > 0 } ?: now
                val lastEdit = tag.lastEdit.takeIf { it > 0 } ?: creation
                queries.insertTag(
                    name = tag.name,
                    creation_date = creation,
                    last_edit = lastEdit
                )
                tagIdMap[tag.id] = queries.lastInsertRowId().executeAsOne()
            }
            data.songs.forEachIndexed { index, song ->
                ensureArtist(song.artist)
                val creation = song.creationDate.takeIf { it > 0 } ?: now
                val lastEdit = song.lastEdit.takeIf { it > 0 } ?: creation
                queries.insertSong(
                    title = song.title,
                    artist = song.artist,
                    key = song.key.ifBlank { null },
                    tempo = song.tempo.ifBlank { null },
                    capo = song.capo.ifBlank { null },
                    duration = song.duration.ifBlank { null },
                    time = song.time.ifBlank { null },
                    body = song.body,
                    youtube_url = song.youtubeUrl.ifBlank { null },
                    sort_order = index.toLong(),
                    creation_date = creation,
                    last_edit = lastEdit,
                    transpose = song.transpose.toLong()
                )
                songIdMap[song.id] = queries.lastInsertRowId().executeAsOne()
            }
            data.songTags.forEach { (songId, tagIds) ->
                val mappedSong = songIdMap[songId] ?: return@forEach
                tagIds.distinct().forEach { tagId ->
                    val mappedTag = tagIdMap[tagId] ?: return@forEach
                    queries.insertSongTag(mappedSong, mappedTag)
                }
            }
            data.setlists.forEach { setlist ->
                val creation = setlist.creationDate.takeIf { it > 0 } ?: now
                val lastEdit = setlist.lastEdit.takeIf { it > 0 } ?: creation
                queries.insertSetlist(
                    name = setlist.name,
                    date = setlist.date.takeIf { it > 0 },
                    location = setlist.location.ifBlank { null },
                    creation_date = creation,
                    last_edit = lastEdit
                )
                setlistIdMap[setlist.id] = queries.lastInsertRowId().executeAsOne()
            }
            data.links.forEach { link ->
                val mappedSetlist = setlistIdMap[link.setlistId] ?: return@forEach
                val mappedSong = songIdMap[link.songId] ?: return@forEach
                queries.insertSetlistSong(mappedSetlist, mappedSong, link.position.toLong())
            }
        }
        return true
    }

    fun importSongs(songs: List<Song>): Int {
        var count = 0
        database.transaction {
            songs.forEach { source ->
                importSongWithDedup(source)
                count++
            }
        }
        return count
    }

    fun importSet(data: SetShareData): Setlist {
        val now = System.currentTimeMillis()
        val newSongs = mutableListOf<Song>()
        val setlistId = database.transactionWithResult {
            val existing = queries.selectSetlistByName(data.setlist.name).executeAsOneOrNull()
            val id: Long
            if (existing != null) {
                queries.updateSetlistInfo(
                    data.setlist.date.takeIf { it > 0 } ?: existing.date,
                    data.setlist.location.ifBlank { null } ?: existing.location,
                    now,
                    existing.id
                )
                queries.deleteAllSetlistSongs(existing.id)
                id = existing.id
            } else {
                queries.insertSetlist(
                    data.setlist.name,
                    data.setlist.date.takeIf { it > 0 },
                    data.setlist.location.ifBlank { null },
                    now,
                    now
                )
                id = queries.lastInsertRowId().executeAsOne()
            }
            data.songs.forEachIndexed { pos, source ->
                val newSong = importSongWithDedup(source)
                newSongs.add(newSong)
                queries.insertSetlistSong(id, newSong.id, pos.toLong())
            }
            id
        }
        return Setlist(
            id = setlistId,
            name = data.setlist.name,
            date = data.setlist.date,
            location = data.setlist.location,
            songs = newSongs
        )
    }

    fun importSetlistHelper(data: SetlistHelperBackup): Pair<Int, Int> {
        var songCount = 0
        var setCount = 0
        database.transaction {
            val idMap = mutableMapOf<Long, Long>()
            data.songs.forEach { source ->
                val newSong = importSongWithDedup(source)
                idMap[source.id] = newSong.id
                data.songTags[source.id]?.let { tagNames ->
                    addTagsToSong(newSong.id, tagNames)
                }
                songCount++
            }
            data.setlists.forEach { helper ->
                val mappedSongIds = helper.songIds.mapNotNull { idMap[it] }
                val existing = if (helper.name.isNotBlank()) {
                    queries.selectSetlistByName(helper.name).executeAsOneOrNull()
                } else {
                    null
                }
                if (existing != null) {
                    queries.updateSetlistInfo(
                        helper.date.takeIf { it > 0 },
                        helper.location.ifBlank { null },
                        System.currentTimeMillis(),
                        existing.id
                    )
                    queries.deleteAllSetlistSongs(existing.id)
                    mappedSongIds.forEachIndexed { pos, songId ->
                        queries.insertSetlistSong(existing.id, songId, pos.toLong())
                    }
                } else {
                    val now = System.currentTimeMillis()
                    queries.insertSetlist(
                        helper.name,
                        helper.date.takeIf { it > 0 },
                        helper.location.ifBlank { null },
                        now,
                        now
                    )
                    val setId = queries.lastInsertRowId().executeAsOne()
                    mappedSongIds.forEachIndexed { pos, songId ->
                        queries.insertSetlistSong(setId, songId, pos.toLong())
                    }
                }
                setCount++
            }
        }
        return songCount to setCount
    }

    /**
     * Importa uma setlist no formato JustChords (.chopro): músicas entram com deduplicação
     * (mesmo título+artista não duplica) e o setlist é criado ou atualizado pelo nome.
     *
     * @return par (músicas importadas, setlists importadas)
     */
    fun importJustChords(data: JustChordsSet): Pair<Int, Int> {
        var songCount = 0
        var setCount = 0
        database.transaction {
            val importedSongIds = data.songs.map { source ->
                importSongWithDedup(source).id
            }
            songCount = importedSongIds.size
            val existing = queries.selectSetlistByName(data.name).executeAsOneOrNull()
            if (existing != null) {
                queries.deleteAllSetlistSongs(existing.id)
                importedSongIds.forEachIndexed { pos, songId ->
                    queries.insertSetlistSong(existing.id, songId, pos.toLong())
                }
            } else {
                val now = System.currentTimeMillis()
                queries.insertSetlist(data.name, null, null, now, now)
                val setId = queries.lastInsertRowId().executeAsOne()
                importedSongIds.forEachIndexed { pos, songId ->
                    queries.insertSetlistSong(setId, songId, pos.toLong())
                }
            }
            setCount = 1
        }
        return songCount to setCount
    }

    private fun importSongWithDedup(source: Song): Song {
        val existing = queries.selectSongByArtistAndTitle(source.title, source.artist).executeAsOneOrNull()
        if (existing != null) {
            updateSongFromSource(existing.id, source)
            return getSong(existing.id) ?: source.copy(id = existing.id)
        }
        return upsert(source.copy(id = 0L))
    }

    private fun addTagsToSong(songId: Long, tagNames: List<String>) {
        val names = tagNames.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (names.isEmpty()) return
        val tagIds = names.map { createTag(it).id }
        val existingIds = queries.selectTagsForSong(songId).executeAsList().map { it.id }
        (existingIds + tagIds).distinct().forEach { tagId ->
            queries.insertSongTag(songId, tagId)
        }
    }

    private fun updateSongFromSource(id: Long, source: Song) {
        val existing = queries.selectSongById(id).executeAsOneOrNull()
        queries.updateSongBody(
            body = source.body,
            title = source.title,
            artist = source.artist,
            key = if (source.key.isBlank()) existing?.key else source.key,
            tempo = if (source.tempo.isBlank()) existing?.tempo else source.tempo,
            capo = if (source.capo.isBlank()) existing?.capo else source.capo,
            duration = if (source.duration.isBlank()) existing?.duration else source.duration,
            time = if (source.time.isBlank()) existing?.time else source.time,
            youtube_url = if (source.youtubeUrl.isBlank()) existing?.youtube_url else source.youtubeUrl,
            last_edit = maxOf(source.lastEdit, System.currentTimeMillis()),
            transpose = if (source.transpose != 0) source.transpose.toLong() else existing?.transpose ?: 0L,
            id = id
        )
    }

    fun allArtists(): List<Artist> =
        queries.selectAllArtists().executeAsList().map { it.toModel() }

    fun createArtist(name: String): Artist {
        val clean = name.trim()
        val existing = queries.selectArtistByName(clean).executeAsOneOrNull()
        if (existing != null) return existing.toModel()
        val now = System.currentTimeMillis()
        queries.insertArtist(clean, now, now)
        val newId = queries.lastInsertRowId().executeAsOne()
        return Artist(id = newId, name = clean, creationDate = now, lastEdit = now)
    }

    fun ensureArtist(name: String): Artist? {
        if (name.isBlank()) return null
        return createArtist(name)
    }

    fun renameArtist(id: Long, newName: String) {
        val existing = queries.selectArtistById(id).executeAsOneOrNull() ?: return
        val clean = newName.trim()
        if (clean.isBlank() || clean == existing.name) return
        database.transaction {
            val now = System.currentTimeMillis()
            queries.renameArtist(clean, now, id)
            queries.updateSongArtist(clean, now, existing.name)
        }
    }

    fun deleteArtist(id: Long) {
        queries.deleteArtist(id)
    }

    fun deleteArtistAndSongs(id: Long) {
        val existing = queries.selectArtistById(id).executeAsOneOrNull() ?: return
        database.transaction {
            queries.deleteSongTagsByArtist(existing.name)
            queries.deleteSetlistSongsByArtist(existing.name)
            queries.deleteSongsByArtist(existing.name)
            queries.deleteArtist(id)
        }
    }

    fun songsByArtist(name: String): List<Song> =
        queries.selectSongsByArtistName(name).executeAsList().map { it.toModel() }

    fun songCountByArtist(): Map<String, Int> =
        queries.songCountByArtist().executeAsList().associate { it.name to it.count.toInt() }

    fun allTags(): List<Tag> =
        queries.selectAllTags().executeAsList().map { it.toModel() }

    fun createTag(name: String): Tag {
        val clean = name.trim()
        val existing = queries.selectTagByName(clean).executeAsOneOrNull()
        if (existing != null) return existing.toModel()
        val now = System.currentTimeMillis()
        queries.insertTag(clean, now, now)
        val newId = queries.lastInsertRowId().executeAsOne()
        return Tag(id = newId, name = clean, creationDate = now, lastEdit = now)
    }

    fun renameTag(id: Long, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        queries.renameTag(clean, System.currentTimeMillis(), id)
    }

    fun deleteTag(id: Long) {
        database.transaction {
            queries.deleteSongTagsByTag(id)
            queries.deleteTag(id)
        }
    }

    fun tagsForSong(songId: Long): List<Tag> =
        queries.selectTagsForSong(songId).executeAsList().map { it.toModel() }

    fun tagsBySong(): Map<Long, List<Tag>> {
        val tagsById = queries.selectAllTags().executeAsList()
            .associateBy { it.id }
            .mapValues { it.value.toModel() }
        val result = mutableMapOf<Long, MutableList<Tag>>()
        queries.selectAllSongTags().executeAsList().forEach { link ->
            val tag = tagsById[link.tag_id] ?: return@forEach
            result.getOrPut(link.song_id) { mutableListOf() }.add(tag)
        }
        return result.mapValues { it.value.sortedBy { t -> t.name.lowercase() } }
    }

    fun songsByTag(tagId: Long): List<Song> =
        queries.selectSongsByTag(tagId).executeAsList().map { it.toModel() }

    fun songCountByTag(): Map<Long, Int> =
        queries.songCountByTag().executeAsList().associate { it.tag_id to it.count.toInt() }

    fun setSongTags(songId: Long, tagIds: List<Long>) {
        database.transaction {
            queries.deleteSongTags(songId)
            tagIds.distinct().forEach { tagId ->
                queries.insertSongTag(songId, tagId)
            }
        }
    }

    private fun DbSong.toModel(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        key = key ?: "",
        tempo = tempo ?: "",
        capo = capo ?: "",
        duration = duration ?: "",
        time = time ?: "",
        youtubeUrl = youtube_url ?: "",
        sortOrder = sort_order,
        body = body,
        creationDate = creation_date,
        lastEdit = last_edit,
        transpose = transpose.toInt()
    )

    private fun DbArtist.toModel(): Artist = Artist(
        id = id,
        name = name,
        creationDate = creation_date,
        lastEdit = last_edit
    )

    private fun DbTag.toModel(): Tag = Tag(
        id = id,
        name = name,
        creationDate = creation_date,
        lastEdit = last_edit
    )

    private fun DbSetlist.toModel(): Setlist = Setlist(
        id = id,
        name = name,
        date = date ?: 0L,
        location = location ?: "",
        songs = songsInSetlist(id),
        creationDate = creation_date,
        lastEdit = last_edit
    )
}
