package com.opensetlist.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.ChordProLine
import com.opensetlist.app.model.ChordProSegment
import com.opensetlist.app.model.CommentStyle
import com.opensetlist.app.model.ParsedSong

/**
 * Renderizador de uma música parseada em ChordPro, com acordes sobrepostos ao texto.
 *
 * @author ruanitto
 */
@Composable
fun ChordProView(
    song: ParsedSong,
    hideChords: Boolean = false,
    fontSize: Float = 14f,
    highlightQuery: String? = null,
    onLineOffset: (index: Int, offsetY: Float) -> Unit = { _, _ -> },
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (song.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.subtitle,
                style = MaterialTheme.typography.titleSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val metaParts = mutableListOf<String>()
        if (song.key.isNotBlank()) metaParts.add(AppStrings.metaKeyValue(AppStrings.metaTom, song.key))
        if (song.tempo.isNotBlank()) metaParts.add(AppStrings.metaKeyValue(AppStrings.metaTempo, song.tempo))
        if (song.time.isNotBlank()) metaParts.add(AppStrings.metaKeyValue(AppStrings.metaCompasso, song.time))
        if (song.duration.isNotBlank()) metaParts.add(AppStrings.metaKeyValue(AppStrings.metaDuracao, song.duration))
        if (song.capo.isNotBlank()) metaParts.add(AppStrings.metaKeyValue(AppStrings.metaCapo, song.capo))

        if (metaParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = metaParts.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val details = mutableListOf<String>()
        if (song.composer.isNotBlank()) details.add(AppStrings.metaKeyValue(AppStrings.metaCompositor, song.composer))
        if (song.lyricist.isNotBlank()) details.add(AppStrings.metaKeyValue(AppStrings.metaLetra, song.lyricist))
        if (song.album.isNotBlank()) details.add(AppStrings.metaKeyValue(AppStrings.metaAlbum, song.album))
        if (song.year.isNotBlank()) details.add(AppStrings.metaKeyValue(AppStrings.metaAno, song.year))
        if (song.copyright.isNotBlank()) details.add("© ${song.copyright}")
        if (song.tags.isNotEmpty()) details.add(AppStrings.metaKeyValue(AppStrings.metaTags, song.tags.joinToString(", ")))

        if (details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = details.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        var visualIndex = 0
        for (line in song.lines) {
            TrackedLine(
                index = visualIndex,
                onOffset = onLineOffset
            ) {
                when {
                    line.isComment -> {
                        CommentLine(
                            text = line.segments.joinToString("") { it.text },
                            style = line.commentStyle,
                            fontSize = fontSize,
                            highlightQuery = highlightQuery
                        )
                    }
                    line.isSection -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "[${line.sectionName}]",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    line.segments.isEmpty() || line.segments.all { it.text.isBlank() && it.chord == null } -> {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    else -> {
                        ChordLine(
                            segments = line.segments,
                            hideChords = hideChords,
                            fontSize = fontSize,
                            highlightQuery = highlightQuery
                        )
                    }
                }
            }
            visualIndex++
        }
    }
}

@Composable
private fun CommentLine(
    text: String,
    style: CommentStyle,
    fontSize: Float,
    highlightQuery: String?
) {
    val isHighlighted = !highlightQuery.isNullOrBlank() &&
        text.contains(highlightQuery, ignoreCase = true)
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightStyle = TextStyle(
        background = MaterialTheme.colorScheme.secondaryContainer
    )
    val textStyle = MaterialTheme.typography.bodyMedium.merge(
        if (isHighlighted) highlightStyle else TextStyle.Default
    )

    when (style) {
        CommentStyle.BOX -> {
            Surface(
                border = BorderStroke(1.dp, baseColor.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.small,
                color = Color.Transparent,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = text,
                    style = textStyle,
                    fontStyle = FontStyle.Italic,
                    color = baseColor,
                    fontSize = fontSize.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        CommentStyle.HIGHLIGHT -> {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(background = MaterialTheme.colorScheme.secondaryContainer)
                ),
                fontStyle = FontStyle.Italic,
                fontSize = fontSize.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        CommentStyle.ITALIC, CommentStyle.PLAIN -> {
            Text(
                text = text,
                style = textStyle,
                fontStyle = FontStyle.Italic,
                fontSize = fontSize.sp,
                color = baseColor,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun TrackedLine(
    index: Int,
    onOffset: (Int, Float) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onOffset(index, coordinates.positionInParent().y)
            }
    ) {
        content()
    }
}

@Composable
private fun ChordLine(
    segments: List<ChordProSegment>,
    hideChords: Boolean,
    fontSize: Float,
    highlightQuery: String?
) {
    val textContent = buildString {
        for (seg in segments) append(seg.text)
    }

    val isHighlighted = !highlightQuery.isNullOrBlank() &&
        textContent.contains(highlightQuery, ignoreCase = true)

    val highlightStyle = TextStyle(
        background = MaterialTheme.colorScheme.secondaryContainer
    )

    if (hideChords) {
        if (textContent.isBlank()) return
        Text(
            text = textContent,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = (fontSize * 1.6f).sp,
            style = if (isHighlighted) highlightStyle else TextStyle.Default
        )
        return
    }

    val hasAnyChord = segments.any { it.chord != null }

    Column {
        if (hasAnyChord) {
            val chordLine = StringBuilder()
            for (seg in segments) {
                chordLine.append(" ".repeat(seg.text.length))
                if (seg.chord != null) {
                    chordLine.append(seg.chord)
                }
            }
            Text(
                text = chordLine.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = (fontSize - 1f).sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = (fontSize * 1.4f).sp,
                style = if (isHighlighted) highlightStyle else TextStyle.Default
            )
        }
        if (textContent.isNotBlank()) {
            Text(
                text = textContent,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = (fontSize * 1.6f).sp,
                style = if (isHighlighted) highlightStyle else TextStyle.Default
            )
        }
    }
}
