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
import com.opensetlist.app.model.SetShareData
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.SetlistHelperBackup
import com.opensetlist.app.model.SetlistSongLink
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag

class SongRepository(private val database: AppDatabase) {
    private val queries = database.appDatabaseQueries

    fun seedIfEmpty() {
        if (queries.selectAllSongs().executeAsList().isNotEmpty()) return
        SampleSongs.songs.forEachIndexed { index, song ->
            ensureArtist(song.artist)
            queries.insertSong(
                song.id, song.title, song.artist, song.key, song.tempo, song.capo,
                song.duration.ifBlank { null }, song.body,
                song.youtubeUrl.ifBlank { null }, index.toLong()
            )
        }
        SampleSongs.allSetlists.forEach { setlist ->
            queries.insertSetlist(
                setlist.id,
                setlist.name,
                setlist.date.ifBlank { null },
                setlist.location.ifBlank { null },
                setlist.time.ifBlank { null }
            )
            setlist.songs.forEachIndexed { pos, song ->
                queries.insertSetlistSong(setlist.id, song.id, pos.toLong())
            }
        }
    }

    fun allSongs(): List<Song> =
        queries.selectAllSongs().executeAsList().map { it.toModel() }

    fun getSong(id: String): Song? =
        queries.selectSongById(id).executeAsOneOrNull()?.toModel()

    fun upsert(song: Song) {
        ensureArtist(song.artist)
        val existing = queries.selectSongById(song.id).executeAsOneOrNull()
        val sortOrder = existing?.sort_order
            ?: queries.selectMaxSortOrder().executeAsOne()
        queries.insertSong(
            id = song.id,
            title = song.title,
            artist = song.artist,
            key = song.key.ifBlank { null },
            tempo = song.tempo.ifBlank { null },
            capo = song.capo.ifBlank { null },
            duration = song.duration.ifBlank { null },
            body = song.body,
            youtube_url = song.youtubeUrl.ifBlank { null },
            sort_order = sortOrder
        )
    }

    fun delete(id: String) {
        database.transaction {
            queries.deleteSetlistSongBySong(id)
            queries.deleteSong(id)
        }
    }

    fun allSetlists(): List<Setlist> =
        queries.selectAllSetlists().executeAsList().map { it.toModel() }

    fun songsInSetlist(setlistId: String): List<Song> =
        queries.selectSongsInSetlist(setlistId).executeAsList().map { it.toModel() }

    fun importSong(body: String): Song {
        val parsed = ChordProParser.parse(body)
        val song = Song(
            id = generateId(),
            title = parsed.title.ifBlank { AppStrings.untitledSong },
            artist = parsed.artist.ifBlank { AppStrings.unknownArtist },
            key = parsed.key,
            tempo = parsed.tempo,
            capo = parsed.capo,
            body = body
        )
        return importSongWithDedup(song)
    }

    fun newSong(): Song = Song(
        id = generateId(),
        title = AppStrings.newSongTitle,
        artist = AppStrings.defaultArtistName,
        key = "",
        tempo = "",
        capo = "",
        body = ""
    )

    fun createSetlist(name: String): Setlist {
        val id = "setlist_${System.currentTimeMillis()}"
        queries.insertSetlist(id, name, null, null, null)
        return Setlist(id = id, name = name, songs = emptyList())
    }

    fun renameSetlist(id: String, name: String) {
        queries.renameSetlist(name, id)
    }

    fun updateSetlistInfo(id: String, date: String, location: String, time: String) {
        queries.updateSetlistInfo(
            date.ifBlank { null },
            location.ifBlank { null },
            time.ifBlank { null },
            id
        )
    }

    fun deleteSetlist(id: String) {
        database.transaction {
            queries.deleteAllSetlistSongs(id)
            queries.deleteSetlist(id)
        }
    }

    fun addSongToSetlist(setlistId: String, songId: String) {
        val position = queries.nextPositionInSetlist(setlistId).executeAsOne()
        queries.insertSetlistSong(setlistId, songId, position)
    }

    fun removeSongFromSetlist(setlistId: String, songId: String) {
        queries.deleteSetlistSong(setlistId, songId)
    }

    fun reorderSetlistSongs(setlistId: String, orderedSongIds: List<String>) {
        database.transaction {
            queries.deleteAllSetlistSongs(setlistId)
            orderedSongIds.forEachIndexed { index, songId ->
                queries.insertSetlistSong(setlistId, songId, index.toLong())
            }
        }
    }

    fun songsNotInSetlist(setlistId: String): List<Song> =
        queries.selectSongsNotInSetlist(setlistId).executeAsList().map { it.toModel() }

    fun backupData(): BackupData {
        val links = queries.selectAllLinks().executeAsList().map {
            SetlistSongLink(it.setlist_id, it.song_id, it.position.toInt())
        }
        return BackupData(allSongs(), allSetlists(), links)
    }

    fun restoreBackup(data: BackupData): Boolean {
        if (data.songs.isEmpty() && data.setlists.isEmpty()) return false
        database.transaction {
            queries.deleteAllSongTags()
            queries.deleteAllLinks()
            queries.deleteAllSetlists()
            queries.deleteAllSongs()
            queries.deleteAllArtists()
            data.artists.forEach { artist ->
                queries.insertArtist(artist.id, artist.name)
            }
            data.tags.forEach { tag ->
                queries.insertTag(tag.id, tag.name)
            }
            data.songs.forEachIndexed { index, song ->
                ensureArtist(song.artist)
                queries.insertSong(
                    song.id, song.title, song.artist,
                    song.key.ifBlank { null }, song.tempo.ifBlank { null },
                    song.capo.ifBlank { null }, song.duration.ifBlank { null },
                    song.body,
                    song.youtubeUrl.ifBlank { null }, index.toLong()
                )
            }
            data.songTags.forEach { (songId, tagIds) ->
                tagIds.distinct().forEach { tagId ->
                    queries.insertSongTag(songId, tagId)
                }
            }
            data.setlists.forEach { setlist ->
                queries.insertSetlist(
                    setlist.id, setlist.name,
                    setlist.date.ifBlank { null },
                    setlist.location.ifBlank { null },
                    setlist.time.ifBlank { null }
                )
            }
            data.links.forEach { link ->
                queries.insertSetlistSong(link.setlistId, link.songId, link.position.toLong())
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
        val setlistId = "setlist_${System.currentTimeMillis()}"
        val newSongs = mutableListOf<Song>()
        database.transaction {
            queries.insertSetlist(
                setlistId, data.setlist.name,
                data.setlist.date.ifBlank { null },
                data.setlist.location.ifBlank { null },
                data.setlist.time.ifBlank { null }
            )
            data.songs.forEachIndexed { pos, source ->
                val newSong = importSongWithDedup(source)
                newSongs.add(newSong)
                queries.insertSetlistSong(setlistId, newSong.id, pos.toLong())
            }
        }
        return Setlist(
            id = setlistId,
            name = data.setlist.name,
            date = data.setlist.date,
            location = data.setlist.location,
            time = data.setlist.time,
            songs = newSongs
        )
    }

    fun importSetlistHelper(data: SetlistHelperBackup): Pair<Int, Int> {
        var songCount = 0
        var setCount = 0
        database.transaction {
            val idMap = mutableMapOf<String, String>()
            data.songs.forEach { source ->
                val newSong = importSongWithDedup(source)
                idMap[source.id] = newSong.id
                songCount++
            }
            data.setlists.forEach { helper ->
                val setId = "setlist_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}"
                queries.insertSetlist(
                    setId, helper.name,
                    helper.date.ifBlank { null },
                    helper.location.ifBlank { null },
                    null
                )
                helper.songIds.forEachIndexed { pos, originalId ->
                    val newSongId = idMap[originalId] ?: return@forEachIndexed
                    queries.insertSetlistSong(setId, newSongId, pos.toLong())
                }
                setCount++
            }
        }
        return songCount to setCount
    }

    private fun importSongWithDedup(source: Song): Song {
        val existing = queries.selectSongByArtistAndTitle(source.title, source.artist).executeAsOneOrNull()
        if (existing != null) {
            updateSongFromSource(existing.id, source)
            return getSong(existing.id) ?: source.copy(id = existing.id)
        }
        val newSong = source.copy(id = generateId())
        upsert(newSong)
        return newSong
    }

    private fun updateSongFromSource(id: String, source: Song) {
        val existing = queries.selectSongById(id).executeAsOneOrNull()
        queries.updateSongBody(
            body = source.body,
            title = source.title,
            artist = source.artist,
            key = source.key.ifBlank { existing?.key ?: null },
            tempo = source.tempo.ifBlank { existing?.tempo ?: null },
            capo = source.capo.ifBlank { existing?.capo ?: null },
            duration = source.duration.ifBlank { existing?.duration ?: null },
            youtube_url = source.youtubeUrl.ifBlank { existing?.youtube_url ?: null },
            id = id
        )
    }

    fun allArtists(): List<Artist> =
        queries.selectAllArtists().executeAsList().map { it.toModel() }

    fun createArtist(name: String): Artist {
        val clean = name.trim()
        val existing = queries.selectArtistByName(clean).executeAsOneOrNull()
        if (existing != null) return existing.toModel()
        val artist = Artist(id = "artist_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}", name = clean)
        queries.insertArtist(artist.id, artist.name)
        return artist
    }

    fun ensureArtist(name: String): Artist? {
        if (name.isBlank()) return null
        return createArtist(name)
    }

    fun renameArtist(id: String, newName: String) {
        val existing = queries.selectArtistById(id).executeAsOneOrNull() ?: return
        val clean = newName.trim()
        if (clean.isBlank() || clean == existing.name) return
        database.transaction {
            queries.renameArtist(clean, id)
            queries.updateSongArtist(clean, existing.name)
        }
    }

    fun deleteArtist(id: String) {
        queries.deleteArtist(id)
    }

    fun deleteArtistAndSongs(id: String) {
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
        val tag = Tag(id = "tag_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}", name = clean)
        queries.insertTag(tag.id, tag.name)
        return tag
    }

    fun renameTag(id: String, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        queries.renameTag(clean, id)
    }

    fun deleteTag(id: String) {
        database.transaction {
            queries.deleteSongTagsByTag(id)
            queries.deleteTag(id)
        }
    }

    fun tagsForSong(songId: String): List<Tag> =
        queries.selectTagsForSong(songId).executeAsList().map { it.toModel() }

    fun tagsBySong(): Map<String, List<Tag>> {
        val tagsById = queries.selectAllTags().executeAsList()
            .associateBy { it.id }
            .mapValues { it.value.toModel() }
        val result = mutableMapOf<String, MutableList<Tag>>()
        queries.selectAllSongTags().executeAsList().forEach { link ->
            val tag = tagsById[link.tag_id] ?: return@forEach
            result.getOrPut(link.song_id) { mutableListOf() }.add(tag)
        }
        return result.mapValues { it.value.sortedBy { t -> t.name.lowercase() } }
    }

    fun songsByTag(tagId: String): List<Song> =
        queries.selectSongsByTag(tagId).executeAsList().map { it.toModel() }

    fun songCountByTag(): Map<String, Int> =
        queries.songCountByTag().executeAsList().associate { it.tag_id to it.count.toInt() }

    fun setSongTags(songId: String, tagIds: List<String>) {
        database.transaction {
            queries.deleteSongTags(songId)
            tagIds.distinct().forEach { tagId ->
                queries.insertSongTag(songId, tagId)
            }
        }
    }

    private fun generateId(): String =
        "song_${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(1000)}"

    private fun DbSong.toModel(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        key = key ?: "",
        tempo = tempo ?: "",
        capo = capo ?: "",
        duration = duration ?: "",
        youtubeUrl = youtube_url ?: "",
        sortOrder = sort_order,
        body = body
    )

    private fun DbArtist.toModel(): Artist = Artist(
        id = id,
        name = name
    )

    private fun DbTag.toModel(): Tag = Tag(
        id = id,
        name = name
    )

    private fun DbSetlist.toModel(): Setlist = Setlist(
        id = id,
        name = name,
        date = date ?: "",
        location = location ?: "",
        time = time ?: "",
        songs = songsInSetlist(id)
    )
}
