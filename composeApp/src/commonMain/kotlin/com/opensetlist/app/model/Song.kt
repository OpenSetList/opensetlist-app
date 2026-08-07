package com.opensetlist.app.model

/**
 * Uma música do repertório, com metadados e corpo em formato ChordPro.
 *
 * @author ruanitto
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val key: String = "",
    val tempo: String = "",
    val capo: String = "",
    val duration: String = "",
    val time: String = "",
    val youtubeUrl: String = "",
    val sortOrder: Long = 0,
    val body: String = "",
    val creationDate: Long = 0L,
    val lastEdit: Long = 0L
)

/**
 * Artista cadastrado no repertório.
 *
 * @author ruanitto
 */
data class Artist(
    val id: Long,
    val name: String,
    val creationDate: Long = 0L,
    val lastEdit: Long = 0L
)

/**
 * Tag utilizada para categorizar músicas.
 *
 * @author ruanitto
 */
data class Tag(
    val id: Long,
    val name: String,
    val creationDate: Long = 0L,
    val lastEdit: Long = 0L
)

/**
 * Uma setlist de apresentação, com dados da gig e as músicas que a compõem.
 *
 * @author ruanitto
 */
data class Setlist(
    val id: Long,
    val name: String,
    val date: String = "",
    val location: String = "",
    val time: String = "",
    val songs: List<Song> = emptyList(),
    val creationDate: Long = 0L,
    val lastEdit: Long = 0L
)

/**
 * Associação entre uma setlist e uma música, com a posição de exibição.
 *
 * @author ruanitto
 */
data class SetlistSongLink(
    val setlistId: Long,
    val songId: Long,
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
    val songTags: Map<Long, List<Long>> = emptyMap()
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
    val setlists: List<HelperSetlist>,
    val songTags: Map<Long, List<String>> = emptyMap()
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
    val songIds: List<Long>
)
