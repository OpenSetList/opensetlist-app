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
    val lastEdit: Long = 0L,
    val transpose: Int = 0
)

/**
 * Artista cadastrado no repertório.
 */
data class Artist(
    val id: Long,
    val name: String,
    val creationDate: Long = 0L,
    val lastEdit: Long = 0L
)

/**
 * Tag utilizada para categorizar músicas.
 */
data class Tag(
    val id: Long,
    val name: String,
    val creationDate: Long = 0L,
    val lastEdit: Long = 0L
)

/**
 * Uma setlist de apresentação, com dados da gig e as músicas que a compõem.
 */
data class Setlist(
    val id: Long,
    val name: String,
    val date: Long = 0L,
    val location: String = "",
    val songs: List<Song> = emptyList(),
    val creationDate: Long = 0L,
    val lastEdit: Long = 0L
)

/**
 * Associação entre uma setlist e uma música, com a posição de exibição.
 */
data class SetlistSongLink(
    val setlistId: Long,
    val songId: Long,
    val position: Int
)

/**
 * Estado completo do banco, usado para exportar/importar backups.
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
 */
data class SetShareData(
    val setlist: Setlist,
    val songs: List<Song>
)

/**
 * Backup no formato do SetList Helper (músicas + setlists).
 */
data class SetlistHelperBackup(
    val songs: List<Song>,
    val setlists: List<HelperSetlist>,
    val songTags: Map<Long, List<String>> = emptyMap()
)

/**
 * Setlist vinda de um backup do SetList Helper.
 */
data class HelperSetlist(
    val name: String,
    val date: Long = 0L,
    val location: String,
    val songIds: List<Long>
)

/**
 * Setlist no formato JustChords (.chopro): o nome do arquivo vira o nome do setlist
 * e cada diretiva `{new_song}` inicia uma nova música na sequência.
 */
data class JustChordsSet(
    val name: String,
    val songs: List<Song>
)
