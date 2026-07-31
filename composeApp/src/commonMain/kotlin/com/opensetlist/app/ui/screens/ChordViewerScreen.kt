package com.opensetlist.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.data.ChordProParser
import com.opensetlist.app.data.Transposer
import com.opensetlist.app.data.pedal.PedalEvent
import com.opensetlist.app.data.pedal.rememberPedalEvents
import com.opensetlist.app.data.rememberFileActions
import com.opensetlist.app.model.ParsedSong
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag
import com.opensetlist.app.ui.components.ChordProView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChordViewerScreen(
    songs: List<Song>,
    initialIndex: Int,
    songTags: Map<String, List<Tag>> = emptyMap(),
    onBack: () -> Unit,
    onEdit: (Song) -> Unit,
    onDelete: (Song) -> Unit,
    onNavigateTo: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeIndex = initialIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeIndex) { songs.size }

    var hideChords by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(14f) }
    var transpose by remember { mutableStateOf(0) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableStateOf(5) }
    var viewportHeight by remember { mutableStateOf(0) }
    var pedalEnabled by remember { mutableStateOf(false) }

    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }

    val scrollStates = remember { mutableStateMapOf<String, androidx.compose.foundation.ScrollState>() }
    val lineOffsets = remember { mutableMapOf<String, MutableMap<Int, Float>>() }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val currentSong = songs.getOrElse(pagerState.currentPage) { songs[safeIndex] }

    val displayBody = remember(currentSong.id, currentSong.body, transpose) {
        Transposer.transposeBody(currentSong.body, transpose)
    }
    val parsedSong = remember(displayBody) { ChordProParser.parse(displayBody) }

    val matches = remember(searchQuery, displayBody, currentSong.id) {
        if (searchQuery.isBlank()) emptyList()
        else parsedSong.lines.mapIndexedNotNull { index, line ->
            val text = line.segments.joinToString("") { it.text }
            val sectionText = if (line.isSection) "[${line.sectionName}]" else ""
            if ((text + sectionText).contains(searchQuery, ignoreCase = true)) index else null
        }
    }

    val fileActions = rememberFileActions(
        getExportContent = { displayBody },
        onImported = {},
        onExported = {},
        onShared = {},
        getExportBytes = { null }
    )

    LaunchedEffect(pagerState.currentPage) {
        onNavigateTo(pagerState.currentPage)
    }

    LaunchedEffect(isAutoScrolling, scrollSpeed, currentSong.id) {
        if (isAutoScrolling) {
            while (true) {
                kotlinx.coroutines.delay(16L)
                scrollStates[currentSong.id]?.scrollBy(scrollSpeed * 0.5f)
            }
        }
    }

    fun scrollToMatch(matchIndex: Int) {
        if (matches.isEmpty() || matchIndex !in matches.indices) return
        val lineIndex = matches[matchIndex]
        val offset = lineOffsets[currentSong.id]?.get(lineIndex)?.toInt() ?: 0
        val state = scrollStates[currentSong.id] ?: return
        scope.launch {
            state.animateScrollTo(offset.coerceIn(0, state.maxValue))
        }
    }

    fun pageBy(direction: Int) {
        val state = scrollStates[currentSong.id] ?: return
        scope.launch {
            val page = (viewportHeight * 0.9f).toInt().coerceAtLeast(100)
            state.animateScrollBy(page.toFloat() * direction)
        }
    }

    val pedalHandler: (PedalEvent) -> Unit = { event ->
        when (event) {
            PedalEvent.NEXT -> pageBy(1)
            PedalEvent.PREVIOUS -> pageBy(-1)
            PedalEvent.PLAY_PAUSE -> isAutoScrolling = !isAutoScrolling
        }
    }

    val pedalState = rememberPedalEvents(onEvent = pedalHandler)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (pagerState.currentPage > 0) {
                            Text(
                                text = songs[pagerState.currentPage - 1].title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .widthIn(max = 120.dp)
                                    .padding(end = 12.dp)
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                            )
                        }
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentSong.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (pagerState.currentPage < songs.lastIndex) {
                            Text(
                                text = songs[pagerState.currentPage + 1].title,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .widthIn(max = 120.dp)
                                    .padding(start = 12.dp)
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.back
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchOpen = !searchOpen }) {
                        Icon(
                            imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = AppStrings.search
                        )
                    }
                    IconButton(onClick = { onEdit(currentSong) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = AppStrings.edit
                        )
                    }
                    IconButton(onClick = { fileActions.saveFile("${currentSong.title}.pro", "application/octet-stream") }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = AppStrings.exportPro
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = !menuOpen }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = AppStrings.moreOptions
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(AppStrings.viewOnYoutube) },
                                enabled = currentSong.youtubeUrl.isNotBlank(),
                                onClick = {
                                    menuOpen = false
                                    fileActions.openUrl(currentSong.youtubeUrl)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(AppStrings.deleteSong) },
                                onClick = {
                                    menuOpen = false
                                    onDelete(currentSong)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ViewerControls(
                transpose = transpose,
                onTransposeChange = { transpose = it },
                fontSize = fontSize,
                onFontSizeChange = { fontSize = it },
                hideChords = hideChords,
                onHideChordsChange = { hideChords = it },
                isAutoScrolling = isAutoScrolling,
                onAutoScrollChange = { isAutoScrolling = it },
                scrollSpeed = scrollSpeed,
                onScrollSpeedChange = { scrollSpeed = it },
                pedalEnabled = pedalEnabled,
                onPedalEnabledChange = { pedalState.setEnabled(it); pedalEnabled = it }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (searchOpen) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                currentMatchIndex = 0
                            },
                            placeholder = { Text(AppStrings.searchInSongPlaceholder) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboard?.hide() }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (matches.isNotEmpty()) {
                                    currentMatchIndex =
                                        (currentMatchIndex - 1 + matches.size) % matches.size
                                    scrollToMatch(currentMatchIndex)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = AppStrings.previous
                            )
                        }
                        IconButton(
                            onClick = {
                                if (matches.isNotEmpty()) {
                                    currentMatchIndex = (currentMatchIndex + 1) % matches.size
                                    scrollToMatch(currentMatchIndex)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = AppStrings.next
                            )
                        }
                        Text(
                            text = if (searchQuery.isBlank() || matches.isEmpty()) {
                                "0/0"
                            } else {
                                "${currentMatchIndex + 1}/${matches.size}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                HorizontalDivider()
            }

            val currentTags = songTags[currentSong.id].orEmpty()
            if (currentTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentTags.forEach { tag ->
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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
            ) { page ->
                val song = songs[page]
                val scrollState = scrollStates.getOrPut(song.id) { rememberScrollState() }
                val offsets = lineOffsets.getOrPut(song.id) { mutableMapOf() }
                val pageParsed: ParsedSong = remember(song.id, song.body, transpose) {
                    ChordProParser.parse(Transposer.transposeBody(song.body, transpose))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewportHeight = it.height }
                        .pinchZoom { zoom ->
                            fontSize = (fontSize * zoom).coerceIn(10f, 40f)
                        }
                ) {
                    ChordProView(
                        song = pageParsed,
                        hideChords = hideChords,
                        fontSize = fontSize,
                        highlightQuery = if (song.id == currentSong.id) {
                            searchQuery.takeIf { it.isNotBlank() }
                        } else null,
                        onLineOffset = { index, offset -> offsets[index] = offset },
                        scrollState = scrollState,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun Modifier.pinchZoom(onZoom: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var multiTouch = false
        do {
            val event = awaitPointerEvent()
            val pressed = event.changes.count { it.pressed }
            if (pressed >= 2) {
                if (multiTouch) onZoom(event.calculateZoom())
                multiTouch = true
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

@Composable
private fun ViewerControls(
    transpose: Int,
    onTransposeChange: (Int) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    hideChords: Boolean,
    onHideChordsChange: (Boolean) -> Unit,
    isAutoScrolling: Boolean,
    onAutoScrollChange: (Boolean) -> Unit,
    scrollSpeed: Int,
    onScrollSpeedChange: (Int) -> Unit,
    pedalEnabled: Boolean,
    onPedalEnabledChange: (Boolean) -> Unit
) {
    Surface(tonalElevation = 3.dp) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onTransposeChange(transpose - 1) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = AppStrings.transposeMinusOne
                    )
                }
                Text(
                    text = if (transpose > 0) "+$transpose" else "$transpose",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { onTransposeChange(transpose + 1) }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = AppStrings.transposePlusOne
                    )
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                IconButton(onClick = { onFontSizeChange((fontSize - 1f).coerceAtLeast(10f)) }) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = AppStrings.decreaseFont
                    )
                }
                Text(
                    text = "A",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { onFontSizeChange((fontSize + 1f).coerceAtMost(26f)) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = AppStrings.increaseFont
                    )
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                IconButton(onClick = { onHideChordsChange(!hideChords) }) {
                    Icon(
                        imageVector = if (hideChords) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hideChords) AppStrings.showChords else AppStrings.hideChords
                    )
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                IconButton(onClick = { onAutoScrollChange(!isAutoScrolling) }) {
                    Icon(
                        imageVector = if (isAutoScrolling) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = AppStrings.autoScroll
                    )
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                IconButton(onClick = { onPedalEnabledChange(!pedalEnabled) }) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = AppStrings.pedalBluetooth,
                        tint = if (pedalEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isAutoScrolling) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.speedLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = { onScrollSpeedChange((scrollSpeed - 1).coerceAtLeast(1)) }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = AppStrings.decreaseSpeed,
                            modifier = Modifier.padding(4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "$scrollSpeed",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { onScrollSpeedChange((scrollSpeed + 1).coerceAtMost(10)) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = AppStrings.increaseSpeed,
                            modifier = Modifier.padding(4.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
