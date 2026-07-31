package com.opensetlist.app.model

/**
 * Uma música do repertório, com metadados e corpo em formato ChordPro.
 *
 * @author ruanitto
 */
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

/**
 * Artista cadastrado no repertório.
 *
 * @author ruanitto
 */
data class Artist(
    val id: String,
    val name: String
)

/**
 * Tag utilizada para categorizar músicas.
 *
 * @author ruanitto
 */
data class Tag(
    val id: String,
    val name: String
)

/**
 * Uma setlist de apresentação, com dados da gig e as músicas que a compõem.
 *
 * @author ruanitto
 */
data class Setlist(
    val id: String,
    val name: String,
    val date: String = "",
    val location: String = "",
    val time: String = "",
    val songs: List<Song> = emptyList()
)

/**
 * Associação entre uma setlist e uma música, com a posição de exibição.
 *
 * @author ruanitto
 */
data class SetlistSongLink(
    val setlistId: String,
    val songId: String,
    val position: Int
)

/**
 * Estado completo do banco, usado para exportar/importar backups.
 *
 * @author ruanitto
 */
data class BackupData(
    val songs: List<Song>,
    val setlists: List<Setlist>,
    val links: List<SetlistSongLink>,
    val artists: List<Artist> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val songTags: Map<String, List<String>> = emptyMap()
)

/**
 * Dados de uma setlist compartilhada: a setlist em si e as músicas a importar.
 *
 * @author ruanitto
 */
data class SetShareData(
    val setlist: Setlist,
    val songs: List<Song>
)

/**
 * Backup no formato do SetList Helper (músicas + setlists).
 *
 * @author ruanitto
 */
data class SetlistHelperBackup(
    val songs: List<Song>,
    val setlists: List<HelperSetlist>
)

/**
 * Setlist vinda de um backup do SetList Helper.
 *
 * @author ruanitto
 */
data class HelperSetlist(
    val name: String,
    val date: String,
    val location: String,
    val songIds: List<String>
)
