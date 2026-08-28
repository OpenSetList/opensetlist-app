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

    /** Índice (0-11) de uma nota na escala de sustenidos. */
    private fun indexOf(note: String): Int {
        val s = sharps.indexOf(note)
        if (s != -1) return s
        val flatPos = flats.indexOf(note)
        if (flatPos != -1) {
            val sharpForm = sharps[flatPos]
            return sharps.indexOf(sharpForm)
        }
        return -1
    }

    fun transposeChord(chord: String, semitones: Int): String {
        if (semitones == 0 || chord.isBlank()) return chord
        val trimmed = chord.trim()
        val root = rootRegex.find(trimmed)?.groupValues?.get(1) ?: return chord
        val rootIndex = indexOf(root)
        if (rootIndex == -1) return chord
        // A notação original da raiz (sustenido ou bemol) é preservada ao transpor.
        val scale = if (root.endsWith("b")) flats else sharps
        val newIndex = ((rootIndex + semitones) % 12 + 12) % 12

        var result = scale[newIndex] + trimmed.substring(root.length)

        // Acorde com baixo (ex.: C/E, Am7/G#): transpõe também a nota do baixo.
        val slash = result.lastIndexOf('/')
        if (slash > -1) {
            val bass = result.substring(slash + 1)
            val bassRoot = rootRegex.find(bass)?.groupValues?.get(1)
            if (bassRoot != null && bass.length == bassRoot.length) {
                val bassIndex = indexOf(bassRoot)
                if (bassIndex != -1) {
                    result = result.substring(0, slash + 1) +
                        scale[((bassIndex + semitones) % 12 + 12) % 12] +
                        bass.substring(bassRoot.length)
                }
            }
        }

        return result
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
