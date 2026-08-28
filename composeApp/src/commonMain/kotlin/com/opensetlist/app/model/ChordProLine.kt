package com.opensetlist.app.model

/**
 * Segmento de texto de uma linha, com um acorde opcional sobreposto.
 *
 * @author ruanitto
 */
data class ChordProSegment(
    val text: String,
    val chord: String? = null
)

/**
 * Estilo de comentário de uma linha ChordPro.
 */
enum class CommentStyle {
    PLAIN,
    ITALIC,
    BOX,
    HIGHLIGHT
}

/**
 * Uma linha renderizável do ChordPro, com seus segmentos e marcadores.
 */
data class ChordProLine(
    val segments: List<ChordProSegment>,
    val isSection: Boolean = false,
    val sectionName: String = "",
    val isComment: Boolean = false,
    val commentStyle: CommentStyle = CommentStyle.PLAIN,
    val isChorus: Boolean = false,
    val isTab: Boolean = false,
    val isHighlight: Boolean = false
)

/**
 * Música parseada no formato ChordPro, com metadados e linhas renderizáveis.
 */
data class ParsedSong(
    val title: String = "",
    val subtitle: String = "",
    val artist: String = "",
    val composer: String = "",
    val lyricist: String = "",
    val copyright: String = "",
    val album: String = "",
    val year: String = "",
    val key: String = "",
    val time: String = "",
    val tempo: String = "",
    val duration: String = "",
    val capo: String = "",
    val youtube: String = "",
    val sorttitle: String = "",
    val tags: List<String> = emptyList(),
    val customMeta: Map<String, String> = emptyMap(),
    val transpose: Int = 0,
    val lines: List<ChordProLine> = emptyList()
)
