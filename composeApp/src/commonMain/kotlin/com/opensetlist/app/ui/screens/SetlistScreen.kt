package com.opensetlist.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.data.formatDuration
import com.opensetlist.app.data.parseDurationSeconds
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.Song
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetlistScreen(
    setlist: Setlist,
    allSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onUpdateInfo: (date: String, location: String, time: String) -> Unit,
    onReorder: (List<Song>) -> Unit,
    onAddSong: (Song) -> Unit,
    onRemoveSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var songs by remember(setlist.id) { mutableStateOf(setlist.songs) }
    var draggingSongId by remember(setlist.id) { mutableStateOf<String?>(null) }
    var dragAccum by remember(setlist.id) { mutableStateOf(0f) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showGigDialog by remember { mutableStateOf(false) }

    val itemHeightPx = with(LocalDensity.current) { 56.dp.toPx() }

    LaunchedEffect(setlist) {
        songs = setlist.songs
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = setlist.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = buildString {
                        append(AppStrings.songsCount(songs.size))
                        val total = songs.sumOf { parseDurationSeconds(it.duration) }
                        val formatted = formatDuration(total)
                        if (formatted.isNotEmpty()) append(" · $formatted")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showGigDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = AppStrings.editGigInfo
                )
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = AppStrings.addSong
                )
            }
        }

        if (setlist.date.isNotBlank() || setlist.location.isNotBlank() || setlist.time.isNotBlank()) {
            Text(
                text = buildString {
                    if (setlist.date.isNotBlank()) append(setlist.date)
                    if (setlist.location.isNotBlank()) {
                        if (isNotEmpty()) append("  ·  ")
                        append(setlist.location)
                    }
                    if (setlist.time.isNotBlank()) {
                        if (isNotEmpty()) append("  ·  ")
                        append(setlist.time)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (songs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = AppStrings.emptySetlist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = AppStrings.dragToReorderHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showAddDialog = true }) {
                    Text(AppStrings.addSongs)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(songs, key = { it.id }) { song ->
                    val index = songs.indexOfFirst { it.id == song.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                if (draggingSongId == song.id) alpha = 0.6f
                            }
                            .pointerInput(song.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingSongId = song.id
                                        dragAccum = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        val idx = songs.indexOfFirst { it.id == song.id }
                                        if (idx < 0) return@detectDragGesturesAfterLongPress
                                        dragAccum += amount.y
                                        val delta = (dragAccum / itemHeightPx).roundToInt()
                                        if (delta != 0) {
                                            val target = (idx + delta).coerceIn(0, songs.lastIndex)
                                            if (target != idx) {
                                                val updated = songs.toMutableList()
                                                updated.add(target, updated.removeAt(idx))
                                                songs = updated
                                                onReorder(updated)
                                                dragAccum = 0f
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingSongId = null
                                        dragAccum = 0f
                                    },
                                    onDragCancel = {
                                        draggingSongId = null
                                        dragAccum = 0f
                                    }
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = AppStrings.reorder,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSongClick(song) }
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onRemoveSong(song) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = AppStrings.removeFromSetlist
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddSongDialog(
            allSongs = allSongs,
            currentSongIds = songs.map { it.id }.toSet(),
            onAdd = { song ->
                onAddSong(song)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showGigDialog) {
        GigInfoDialog(
            date = setlist.date,
            location = setlist.location,
            time = setlist.time,
            onSave = { date, location, time ->
                onUpdateInfo(date, location, time)
                showGigDialog = false
            },
            onDismiss = { showGigDialog = false }
        )
    }
}

@Composable
private fun GigInfoDialog(
    date: String,
    location: String,
    time: String,
    onSave: (date: String, location: String, time: String) -> Unit,
    onDismiss: () -> Unit
) {
    var dateText by remember { mutableStateOf(date) }
    var locationText by remember { mutableStateOf(location) }
    var timeText by remember { mutableStateOf(time) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.editGigInfo) },
        text = {
            Column {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text(AppStrings.dateLabel) },
                    placeholder = { Text(AppStrings.datePlaceholder) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text(AppStrings.locationLabel) },
                    placeholder = { Text(AppStrings.locationPlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text(AppStrings.timeLabel) },
                    placeholder = { Text(AppStrings.timePlaceholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(dateText.trim(), locationText.trim(), timeText.trim())
            }) {
                Text(AppStrings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel)
            }
        }
    )
}

@Composable
private fun AddSongDialog(
    allSongs: List<Song>,
    currentSongIds: Set<String>,
    onAdd: (Song) -> Unit,
    onDismiss: () -> Unit
) {
    val available = allSongs.filter { it.id !in currentSongIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.addSongs) },
        text = {
            if (available.isEmpty()) {
                Text(
                    text = AppStrings.allSongsInSetlist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(available, key = { _, s -> s.id }) { _, song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAdd(song) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.close)
            }
        }
    )
}
