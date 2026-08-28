package com.opensetlist.app.data

import com.opensetlist.app.model.BackupData
import com.opensetlist.app.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DataTransferTest {

    private val backupJson = """
        {
          "type": "setlist_app_backup",
          "version": 4,
          "createdAt": "2026-07-31T14:30:00",
          "songs": [
            {"id":1,"title":"A","artist":"X","key":"C","tempo":"","capo":"","duration":"","time":"",
             "youtubeUrl":"","body":"[G] verso","creationDate":0,"lastEdit":0,"transpose":0}
          ],
          "setlists": [
            {"id":10,"name":"Gig 1","date":0,"location":"","creationDate":0,"lastEdit":0}
          ],
          "links": [
            {"setlistId":10,"songId":1,"position":0}
          ]
        }
    """.trimIndent()

    @Test
    fun detectType_backup() {
        assertEquals("backup", DataTransfer.detectType(backupJson))
    }

    @Test
    fun detectType_songs() {
        val json = """{"type":"setlist_app_songs","version":1,"songs":[]}"""
        assertEquals("songs", DataTransfer.detectType(json))
    }

    @Test
    fun detectType_set() {
        val json = """{"type":"setlist_app_set","version":1,"setlist":{},"songs":[]}"""
        assertEquals("set", DataTransfer.detectType(json))
    }

    @Test
    fun detectType_unknownOrInvalid() {
        assertNull(DataTransfer.detectType("""{"type":"other"}"""))
        assertNull(DataTransfer.detectType("not json"))
        assertNull(DataTransfer.detectType("{}"))
    }

    @Test
    fun parseBackupJson_populatesSongsSetlistsLinks() {
        val data = DataTransfer.parseBackupJson(backupJson)
        assertNotNull(data)
        assertEquals(1, data.songs.size)
        assertEquals("A", data.songs[0].title)
        assertEquals("[G] verso", data.songs[0].body)
        assertEquals(1, data.setlists.size)
        assertEquals("Gig 1", data.setlists[0].name)
        assertEquals(1, data.links.size)
        assertEquals(10L, data.links[0].setlistId)
        assertEquals(1L, data.links[0].songId)
    }

    @Test
    fun parseBackupJson_wrongType_returnsNull() {
        val json = """{"type":"setlist_app_songs","songs":[]}"""
        assertNull(DataTransfer.parseBackupJson(json))
    }

    @Test
    fun parseSongsBundleJson_returnsSongs() {
        val json = """{"type":"setlist_app_songs","version":1,"songs":[
            {"id":1,"title":"B","artist":"","key":"","tempo":"","capo":"","duration":"","time":"",
             "youtubeUrl":"","body":"","creationDate":0,"lastEdit":0,"transpose":0}
        ]}"""
        val songs = DataTransfer.parseSongsBundleJson(json)
        assertNotNull(songs)
        assertEquals(1, songs.size)
        assertEquals("B", songs[0].title)
    }
}
