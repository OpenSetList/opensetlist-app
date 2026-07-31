package com.opensetlist.app.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val key: String = "",
    val tempo: String = "",
    val capo: String = "",
    val duration: String = "",
    val youtubeUrl: String = "",
    val sortOrder: Long = 0,
    val body: String = ""
)

data class Artist(
    val id: String,
    val name: String
)

data class Tag(
    val id: String,
    val name: String
)

data class Setlist(
    val id: String,
    val name: String,
    val date: String = "",
    val location: String = "",
    val time: String = "",
    val songs: List<Song> = emptyList()
)

data class SetlistSongLink(
    val setlistId: String,
    val songId: String,
    val position: Int
)

data class BackupData(
    val songs: List<Song>,
    val setlists: List<Setlist>,
    val links: List<SetlistSongLink>,
    val artists: List<Artist> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val songTags: Map<String, List<String>> = emptyMap()
)

data class SetShareData(
    val setlist: Setlist,
    val songs: List<Song>
)

data class SetlistHelperBackup(
    val songs: List<Song>,
    val setlists: List<HelperSetlist>
)

data class HelperSetlist(
    val name: String,
    val date: String,
    val location: String,
    val songIds: List<String>
)
