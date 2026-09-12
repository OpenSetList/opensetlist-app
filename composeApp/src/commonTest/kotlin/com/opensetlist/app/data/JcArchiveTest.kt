package com.opensetlist.app.data

import com.opensetlist.app.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JcArchiveTest {

    private val songs = listOf(
        Song(id = 1, title = "Alegria", artist = "Católicas", key = "G", body = "[G] verso", time = "4/4", duration = "1:30"),
        Song(id = 2, title = "Barca", artist = "Vida Reluz", key = "Am", body = "[Am] onda")
    )

    @Test
    fun buildSongsJson_includesRequiredTopLevelKeys() {
        val root = JsonParser(JcArchive.buildSongsJson(songs, "Gig 1")).parseObject()
        assertTrue(root != null)
        assertEquals(5, root.size)
        assertTrue(root["songs"] is List<*>)
        assertTrue(root["identity"] is String)
        assertTrue(root["tags"] is List<*>)
        assertTrue(root["dataVersion"] is String)
    }

    @Test
    fun buildSongsJson_playlistReferencesSongIds() {
        val root = JsonParser(JcArchive.buildSongsJson(songs, "Gig 1")).parseObject()
        val songsList = root!!["songs"] as List<*>
        val playlists = root["playlists"] as List<*>
        val arrangement = (playlists[0] as Map<*, *>)["arrangement"] as List<*>

        val songIds = songsList.map { (it as Map<*, *>)["id"] }
        val arrangementIds = arrangement.map { (it as Map<*, *>)["id"] }
        assertEquals(songIds, arrangementIds)
        arrangement.forEach { item ->
            val map = item as Map<*, *>
            assertEquals("song", map["type"])
            assertTrue(map["index"] is String)
        }
        val playlist = playlists[0] as Map<*, *>
        assertEquals("Gig 1", playlist["title"])
    }

    @Test
    fun buildSongsJson_withoutSetlist_hasEmptyPlaylists() {
        val root = JsonParser(JcArchive.buildSongsJson(songs)).parseObject()
        assertTrue(root != null)
        assertEquals(emptyList<Any>(), root["playlists"])
    }
}