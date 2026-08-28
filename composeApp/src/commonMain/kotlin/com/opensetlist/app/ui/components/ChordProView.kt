package com.opensetlist.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.ChordProLine
import com.opensetlist.app.model.ChordProSegment
import com.opensetlist.app.model.CommentStyle
import com.opensetlist.app.model.ParsedSong
import com.opensetlist.app.model.Tag

/**
 * Renderizador de uma música parseada em ChordPro, com acordes sobrepostos ao texto.
 *
 * @author ruanitto
 */
@Composable
fun ChordProView(
    song: ParsedSong,
    tags: List<Tag>,
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
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp)
    ) {
        if (tags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = tag.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
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
                val content: @Composable () -> Unit = {
                    when {
                        line.isTab -> {
                            TabLine(
                                segments = line.segments,
                                fontSize = fontSize
                            )
                        }
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
                                color = if (line.isChorus) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
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
                if (line.isChorus) {
                    ChorusBox {
                        if (line.isHighlight) {
                            HighlightBox { content() }
                        } else {
                            content()
                        }
                    }
                } else if (line.isHighlight) {
                    HighlightBox { content() }
                } else {
                    content()
                }
            }
            visualIndex++
        }
    }
}

/**
 * Envolve uma linha do coro com a barra lateral vertical à esquerda (igual ao
 * ChordPro) e um recuo. As barras de linhas consecutivas se unem em uma única
 * barra contínua.
 */
@Composable
private fun ChorusBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val barWidth = with(density) { 3.dp.toPx() }
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
            .padding(start = 14.dp)
    ) {
        content()
    }
}

@Composable
private fun TabLine(
    segments: List<ChordProSegment>,
    fontSize: Float
) {
    val textContent = buildString {
        for (seg in segments) append(seg.text)
    }
    Text(
        text = if (textContent.isBlank()) " " else textContent,
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.5f).sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HighlightBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        content()
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

@OptIn(ExperimentalLayoutApi::class)
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

    val highlightStyle = if (isHighlighted) {
        TextStyle(background = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        TextStyle.Default
    }

    if (hideChords || segments.none { it.chord != null }) {
        if (textContent.isBlank()) return
        Text(
            text = textContent,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = (fontSize * 1.6f).sp,
            style = highlightStyle
        )
        return
    }

    // Linha com apenas acordes e sem letra: mantém a renderização legada.
    if (textContent.isBlank()) {
        val chordLine = StringBuilder()
        for (seg in segments) {
            chordLine.append(" ".repeat(seg.text.length))
            if (seg.chord != null) chordLine.append(seg.chord)
        }
        Text(
            text = chordLine.toString(),
            fontFamily = FontFamily.Monospace,
            fontSize = (fontSize - 1f).sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            lineHeight = (fontSize * 1.4f).sp,
            style = highlightStyle
        )
        return
    }

    // Cada palavra é um bloco atômico de fluxo: acorde + letra viajam juntos
    // na quebra de linha. Dentro da palavra, os acordes são posicionados na
    // coluna exata da sílaba, sem criar espaços artificiais na letra.
    val words = buildLyricWords(segments, textContent)

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        words.forEach { word ->
            LyricWord(
                word = word,
                fontSize = fontSize,
                isHighlighted = isHighlighted
            )
        }
    }
}

/**
 * Uma palavra da letra com os acordes posicionados por índice relativo ao
 * início da palavra (coluna da sílaba em que o acorde foi colocado).
 */
private data class LyricWord(
    val text: String,
    val chords: List<Pair<Int, String>>
)

/**
 * Divide a linha em palavras (blocos atômicos) e associa a cada uma os
 * acordes cuja coluna cai dentro dela, na posição relativa da sílaba.
 */
private fun buildLyricWords(segments: List<ChordProSegment>, textContent: String): List<LyricWord> {
    val chordCols = mutableListOf<Pair<Int, String>>()
    var column = 0
    for (seg in segments) {
        if (seg.chord != null) chordCols.add(column to seg.chord!!)
        column += seg.text.length
    }

    val text = textContent
    val ranges = mutableListOf<Pair<Int, Int>>()
    var wordStart = -1
    var j = 0
    while (j <= text.length) {
        val isSpace = j < text.length && text[j].isWhitespace()
        if (!isSpace && wordStart < 0) wordStart = j
        if ((isSpace || j == text.length) && wordStart >= 0) {
            ranges.add(wordStart to j)
            wordStart = -1
        }
        j++
    }

    val words = mutableListOf<LyricWord>()
    var used = 0
    for ((start, end) in ranges) {
        if (start == end) continue
        val chordsHere = mutableListOf<Pair<Int, String>>()
        while (used < chordCols.size && chordCols[used].first < end) {
            val (c, ch) = chordCols[used]
            if (c >= start) chordsHere.add((c - start) to ch)
            used++
        }
        words.add(LyricWord(text.substring(start, end), chordsHere))
    }

    // Acordes após a última palavra (ex.: [G] no fim da linha) penduram nela.
    val leftover = chordCols.drop(used).map { it.second }
    if (leftover.isNotEmpty() && words.isNotEmpty()) {
        val last = words.last()
        words[words.lastIndex] = LyricWord(
            text = last.text,
            chords = last.chords + leftover.map { last.text.length to it }
        )
    }

    return words
}

/**
 * Renderiza uma palavra como bloco indivisível: uma faixa de acordes fixa por
 * cima (para alinhar a linha de base de todas as palavras) e o acorde desenhado
 * na coluna exata da sílaba dentro da própria palavra.
 */
@Composable
private fun LyricWord(
    word: LyricWord,
    fontSize: Float,
    isHighlighted: Boolean
) {
    val density = LocalDensity.current
    val chordBandDp = with(density) { (fontSize * 1.4f).sp.toPx().toDp() }
    val textMeasurer = rememberTextMeasurer()
    val chordStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = (fontSize - 1f).sp,
        lineHeight = (fontSize * 1.15f).sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        background = if (isHighlighted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Unspecified
        }
    )

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = Modifier.padding(horizontal = 2.dp)) {
        Column {
            Spacer(modifier = Modifier.height(chordBandDp))
            Text(
                text = word.text,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = (fontSize * 2.4f).sp,
                style = if (isHighlighted) {
                    TextStyle(background = MaterialTheme.colorScheme.secondaryContainer)
                } else {
                    TextStyle.Default
                },
                onTextLayout = { layoutResult = it }
            )
        }
        if (word.chords.isNotEmpty()) {
            WordChordOverlay(
                modifier = Modifier.matchParentSize(),
                layoutResult = layoutResult,
                textLength = word.text.length,
                chords = word.chords,
                textMeasurer = textMeasurer,
                chordStyle = chordStyle
            )
        }
    }
}

/**
 * Desenha os acordes da palavra acima da sílaba de cada acorde, na coluna exata.
 */
@Composable
private fun WordChordOverlay(
    modifier: Modifier,
    layoutResult: TextLayoutResult?,
    textLength: Int,
    chords: List<Pair<Int, String>>,
    textMeasurer: TextMeasurer,
    chordStyle: TextStyle
) {
    Canvas(modifier = modifier) {
        if (layoutResult == null || chords.isEmpty() || textLength <= 0) return@Canvas
        val chordHeight = textMeasurer.measure("A", chordStyle).size.height.toFloat().coerceAtLeast(1f)
        val chordGap = 2.dp.toPx()
        var lastChordRight = Float.NEGATIVE_INFINITY
        for ((column, chord) in chords) {
            val offset = column.coerceIn(0, (textLength - 1).coerceAtLeast(0))
            val rect = layoutResult.getBoundingBox(offset)
            val chordWidth = textMeasurer.measure(chord, chordStyle).size.width.toFloat()
            val topLeft = Offset(
                x = maxOf(rect.left, lastChordRight + chordGap),
                y = (rect.top - chordHeight - chordGap).coerceAtLeast(0f)
            )
            lastChordRight = topLeft.x + chordWidth
            if (topLeft.x >= size.width || topLeft.y >= size.height) continue
            drawText(
                textMeasurer = textMeasurer,
                text = chord,
                topLeft = topLeft,
                style = chordStyle
            )
        }
    }
}
