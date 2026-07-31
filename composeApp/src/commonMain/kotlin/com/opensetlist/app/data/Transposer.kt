package com.opensetlist.app.data

/**
 * Transposição de acordes e tom entre semitons.
 *
 * @author ruanitto
 */
object Transposer {
    private val sharps = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val flats = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")
    private val rootRegex = Regex("^([A-G](?:b|#)?)")
    private val chordRegex = Regex("\\[([^\\]]+)]")
    private val keyDirectiveRegex = Regex("(?i)(\\{key:\\s*)([A-G](?:b|#)?[A-Za-z0-9]*)(\\})")

    fun transposeChord(chord: String, semitones: Int): String {
        if (semitones == 0 || chord.isBlank()) return chord
        val root = rootRegex.find(chord.trim())?.groupValues?.get(1) ?: return chord
        val suffix = chord.substring(root.length)
        val scale = if (root.endsWith("b")) flats else sharps
        val index = scale.indexOf(root)
        if (index == -1) return chord
        val newIndex = ((index + semitones) % 12 + 12) % 12
        return scale[newIndex] + suffix
    }

    fun transposeBody(body: String, semitones: Int): String {
        if (semitones == 0) return body
        val chordsTransposed = chordRegex.replace(body) { match ->
            "[${transposeChord(match.groupValues[1].trim(), semitones)}]"
        }
        return keyDirectiveRegex.replace(chordsTransposed) { match ->
            match.groupValues[1] + transposeChord(match.groupValues[2], semitones) + match.groupValues[3]
        }
    }
}
