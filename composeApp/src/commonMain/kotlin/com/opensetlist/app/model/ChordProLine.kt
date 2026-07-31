package com.opensetlist.app.model

data class ChordProSegment(
    val text: String,
    val chord: String? = null
)

enum class CommentStyle {
    PLAIN,
    ITALIC,
    BOX,
    HIGHLIGHT
}

data class ChordProLine(
    val segments: List<ChordProSegment>,
    val isSection: Boolean = false,
    val sectionName: String = "",
    val isComment: Boolean = false,
    val commentStyle: CommentStyle = CommentStyle.PLAIN
)

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
    val sorttitle: String = "",
    val tags: List<String> = emptyList(),
    val customMeta: Map<String, String> = emptyMap(),
    val lines: List<ChordProLine> = emptyList()
)
