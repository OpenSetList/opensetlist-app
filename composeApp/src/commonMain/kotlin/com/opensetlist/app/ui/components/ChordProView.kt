package com.opensetlist.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
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

    // Pré-quebra a linha em pedaços que cabem na largura disponível, alinhados
    // a limites de palavra (assim o acorde nunca se separa da sílaba). Cada
    // pedaço é renderizado como faixa de acordes + linha de letra, de forma
    // determinística (monoespaçada), sem overlay de canvas.
    val words = buildLyricWords(segments, textContent)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val lyricStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 2.4f).sp
    )
    val lyricAdvancePx = with(density) {
        measurer.measure("M", lyricStyle).size.width.toFloat()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val chunks = breakChunks(words, measurer, lyricStyle, maxWidthPx).map { buildChunk(it) }
        Column {
            chunks.forEach { chunk ->
                ChunkLine(
                    chunk = chunk,
                    fontSize = fontSize,
                    lyricAdvancePx = lyricAdvancePx,
                    isHighlighted = isHighlighted
                )
            }
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
 * Um pedaço da linha já quebrado: texto da letra + acordes com coluna absoluta
 * (x = coluna * avanço mono) relativa ao início do pedaço.
 */
private data class Chunk(
    val text: String,
    val chords: List<Pair<Int, String>>
)

/**
 * Quebra a lista de palavras em pedaços que cabem na largura, medindo o texto
 * acumulado e quebrando quando ele vira para uma segunda linha visual.
 */
private fun breakChunks(
    words: List<LyricWord>,
    measurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Float
): List<List<LyricWord>> {
    val chunks = mutableListOf<List<LyricWord>>()
    var current = mutableListOf<LyricWord>()
    for (word in words) {
        val test = current + word
        val text = test.joinToString(" ") { it.text }
        val laid = measurer.measure(
            text,
            style,
            constraints = Constraints(maxWidth = maxWidthPx.toInt() + 1)
        )
        if (current.isNotEmpty() && laid.lineCount > 1) {
            chunks.add(current)
            current = mutableListOf(word)
        } else {
            current = test.toMutableList()
        }
    }
    if (current.isNotEmpty()) chunks.add(current)
    return chunks
}

/**
 * Monta o texto do pedaço e as colunas absolutas dos acordes (relativas ao
 * início do pedaço, em caracteres mono).
 */
private fun buildChunk(words: List<LyricWord>): Chunk {
    val text = StringBuilder()
    val chords = mutableListOf<Pair<Int, String>>()
    var cursor = 0
    words.forEachIndexed { i, w ->
        if (i > 0) {
            text.append(' ')
            cursor++
        }
        w.chords.forEach { (rel, ch) -> chords.add((cursor + rel) to ch) }
        text.append(w.text)
        cursor += w.text.length
    }
    return Chunk(text.toString(), chords)
}

/**
 * Renderiza um pedaço: uma faixa de acordes fixa por cima e a letra embaixo.
 * Os acordes são posicionados na coluna exata da sílaba (x = coluna × avanço).
 */
@Composable
private fun ChunkLine(
    chunk: Chunk,
    fontSize: Float,
    lyricAdvancePx: Float,
    isHighlighted: Boolean
) {
    val density = LocalDensity.current
    val bandDp = with(density) { (fontSize * 1.6f).sp.toPx().toDp() }
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
    val lyricTextStyle = if (isHighlighted) {
        TextStyle(background = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        TextStyle.Default
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bandDp)
        ) {
            chunk.chords.forEach { (colAbs, chord) ->
                Text(
                    text = chord,
                    style = chordStyle,
                    modifier = Modifier.offset(x = with(density) { (colAbs * lyricAdvancePx).toDp() })
                )
            }
        }
        Text(
            text = chunk.text,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = (fontSize * 2.4f).sp,
            style = lyricTextStyle
        )
    }
}
