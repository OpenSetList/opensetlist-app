package com.opensetlist.app.data

/**
 * Mapeamento dos campos do backup do SetList Helper para o formato do app.
 *
 * @author ruanitto
 */
object SetlistHelperMapping {

    /**
     * Converte numerador/denominador de compasso do SetList Helper em "top/bottom".
     */
    fun buildTimeSignature(top: Long?, bottom: Long?): String {
        val t = top ?: return ""
        val b = bottom ?: return ""
        if (t <= 0 || b <= 0) return ""
        return "$t/$b"
    }

    /**
     * Monta o corpo ChordPro do backup: observações e comentários viram
     * diretivas de comentário, seguidos da letra original.
     */
    fun buildImportBody(notes: String, other: String, lyrics: String): String {
        val parts = mutableListOf<String>()
        notes.lines().map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { parts.add("{comment: $it}") }
        other.lines().map { it.trim() }.filter { it.isNotEmpty() }
            .forEach { parts.add("{comment: $it}") }
        if (lyrics.isNotBlank()) parts.add(lyrics)
        return parts.joinToString("\n")
    }
}
