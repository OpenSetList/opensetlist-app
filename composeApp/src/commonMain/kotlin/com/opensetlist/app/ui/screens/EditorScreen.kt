package com.opensetlist.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.data.ChordProParser
import com.opensetlist.app.data.parseDurationSeconds
import com.opensetlist.app.data.setChordProDirective
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag

private val MUSIC_NOTES = listOf(
    "C", "Cm", "C#", "C#m", "D", "Dm", "D#", "D#m", "E", "Em", "F", "Fm",
    "F#", "F#m", "G", "Gm", "G#", "G#m", "A", "Am", "A#", "A#m", "B", "Bm"
)

/**
 * Tela de edição/criação de música no formato ChordPro.
 *
 * @author ruanitto
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    song: Song,
    allTags: List<Tag>,
    initialTags: List<Tag>,
    artistSuggestions: List<String>,
    onSave: (Song, List<Long>) -> Unit,
    onNewTag: (String) -> Unit,
    onCancel: () -> Unit,
    onDelete: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var body by remember(song.id) { mutableStateOf(song.body) }
    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var key by remember(song.id) { mutableStateOf(song.key) }
    var keyMenuOpen by remember(song.id) { mutableStateOf(false) }
    var tempo by remember(song.id) { mutableStateOf(song.tempo) }
    var capo by remember(song.id) { mutableStateOf(song.capo) }
    var transpose by remember(song.id) { mutableStateOf(song.transpose) }
    var youtubeUrl by remember(song.id) { mutableStateOf(song.youtubeUrl) }
    var selectedTagIds by remember(song.id) { mutableStateOf(initialTags.map { it.id }.toSet()) }
    var newTagText by remember { mutableStateOf("") }
    var artistMenuOpen by remember(song.id) { mutableStateOf(false) }

    val initialTimeSplit = remember(song.id) { splitTimeSignature(song.time) }
    var timeNum by remember(song.id) { mutableStateOf(initialTimeSplit.first) }
    var timeDen by remember(song.id) { mutableStateOf(initialTimeSplit.second) }

    val initialDurationSplit = remember(song.id) { splitDurationField(song.duration) }
    var durationMin by remember(song.id) { mutableStateOf(initialDurationSplit.first) }
    var durationSec by remember(song.id) { mutableStateOf(initialDurationSplit.second) }

    val filteredArtistSuggestions = remember(artist, artistSuggestions) {
        artistSuggestions
            .filter { it.contains(artist.trim(), ignoreCase = true) }
            .distinct()
            .sortedBy { it.lowercase() }
    }

    fun save() {
        val parsed = ChordProParser.parse(body)
        val computedTime = when {
            timeNum.isBlank() && timeDen.isBlank() -> ""
            timeNum.isBlank() -> "/${timeDen.trim()}"
            timeDen.isBlank() -> timeNum.trim()
            else -> "${timeNum.trim()}/${timeDen.trim()}"
        }
        val computedDuration = when {
            durationMin.isBlank() && durationSec.isBlank() -> ""
            durationSec.isBlank() -> durationMin.trim()
            durationMin.isBlank() -> "0:${durationSec.trim().padStart(2, '0')}"
            else -> "${durationMin.trim()}:${durationSec.trim().padStart(2, '0')}"
        }
        val updated = song.copy(
            body = body,
            title = title.trim().ifBlank { parsed.title.ifBlank { song.title } },
            artist = artist.trim().ifBlank { parsed.artist.ifBlank { song.artist } },
            key = key.trim().ifBlank { parsed.key.ifBlank { song.key } },
            tempo = tempo.trim().ifBlank { parsed.tempo.ifBlank { song.tempo } },
            capo = capo.trim().ifBlank { parsed.capo.ifBlank { song.capo } },
            duration = computedDuration.ifBlank { parsed.duration.ifBlank { song.duration } },
            time = computedTime.ifBlank { parsed.time.ifBlank { song.time } },
            youtubeUrl = youtubeUrl.trim(),
            transpose = transpose.takeIf { it != 0 } ?: parsed.transpose.takeIf { it > 0 } ?: 0
        )
        onSave(updated, selectedTagIds.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = AppStrings.editSongTitle(song.title),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = AppStrings.cancel
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onDelete(song) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = AppStrings.deleteSong
                        )
                    }
                    TextButton(onClick = ::save) {
                        Text(AppStrings.save)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(AppStrings.titleLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(
                expanded = artistMenuOpen && filteredArtistSuggestions.isNotEmpty(),
                onExpandedChange = { artistMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = artist,
                    onValueChange = {
                        artist = it
                        artistMenuOpen = true
                    },
                    label = { Text(AppStrings.artistLabel) },
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = artistMenuOpen)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = artistMenuOpen && filteredArtistSuggestions.isNotEmpty(),
                    onDismissRequest = { artistMenuOpen = false }
                ) {
                    filteredArtistSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion) },
                            onClick = {
                                artist = suggestion
                                artistMenuOpen = false
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = keyMenuOpen,
                    onExpandedChange = { keyMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.keyLabel) },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = keyMenuOpen) },
                        modifier = Modifier
                            .width(100.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = keyMenuOpen,
                        onDismissRequest = { keyMenuOpen = false }
                    ) {
                        MUSIC_NOTES.forEach { note ->
                            DropdownMenuItem(
                                text = { Text(note) },
                                onClick = {
                                    key = note
                                    keyMenuOpen = false
                                }
                            )
                        }
                    }
                }
                Column {
                    OutlinedTextField(
                        value = capo,
                        onValueChange = { newCapo ->
                            val filtered = newCapo.filter { c -> c.isDigit() }.take(2)
                            capo = filtered
                            body = setChordProDirective(body, "capo", filtered.ifBlank { null })
                        },
                        label = { Text(AppStrings.capoLabel) },
                        singleLine = true,
                        textStyle = TextStyle(textAlign = TextAlign.End),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(84.dp)
                    )
                }
                Column {
                    OutlinedTextField(
                        value = tempo,
                        onValueChange = { tempo = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text(AppStrings.bpmLabel) },
                        singleLine = true,
                        textStyle = TextStyle(textAlign = TextAlign.End),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(96.dp)
                    )
                }
                Column {
                    OutlinedTextField(
                        value = transpose.toString(),
                        onValueChange = { newTranspose ->
                            val filtered = newTranspose.filter { c -> c.isDigit() }.take(2)
                            transpose = filtered.toIntOrNull() ?: 0
                            body = setChordProDirective(
                                body,
                                "transpose",
                                transpose.takeIf { it != 0 }?.toString()
                            )
                        },
                        label = { Text(AppStrings.transposeLabel) },
                        singleLine = true,
                        textStyle = TextStyle(textAlign = TextAlign.End),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.compassoLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = timeNum,
                            onValueChange = { timeNum = it.filter { c -> c.isDigit() }.take(1) },
                            singleLine = true,
                            textStyle = TextStyle(textAlign = TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(60.dp)
                        )
                        Text("/", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = timeDen,
                            onValueChange = { timeDen = it.filter { c -> c.isDigit() }.take(1) },
                            singleLine = true,
                            textStyle = TextStyle(textAlign = TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(60.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.durationLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = durationMin,
                            onValueChange = { durationMin = it.filter { c -> c.isDigit() }.take(3) },
                            singleLine = true,
                            textStyle = TextStyle(textAlign = TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = durationSec,
                            onValueChange = { durationSec = it.filter { c -> c.isDigit() }.take(2) },
                            singleLine = true,
                            textStyle = TextStyle(textAlign = TextAlign.End),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = { youtubeUrl = it },
                label = { Text(AppStrings.youtubeLinkLabel) },
                placeholder = { Text(AppStrings.youtubePlaceholder) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Text(
                text = AppStrings.tagsTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            if (allTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = tag.id in selectedTagIds,
                            onClick = {
                                selectedTagIds = if (tag.id in selectedTagIds) {
                                    selectedTagIds - tag.id
                                } else {
                                    selectedTagIds + tag.id
                                }
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    placeholder = { Text(AppStrings.newTagPlaceholder) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val name = newTagText.trim()
                        if (name.isNotEmpty()) {
                            onNewTag(name)
                            newTagText = ""
                        }
                    },
                    enabled = newTagText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = AppStrings.addTag
                    )
                }
            }

            Text(
                text = AppStrings.bodyLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}

private fun splitTimeSignature(time: String): Pair<String, String> {
    val parts = time.trim().split("/", limit = 2)
    if (parts.size != 2) return "" to ""
    return parts[0].trim() to parts[1].trim()
}

private fun splitDurationField(duration: String): Pair<String, String> {
    val seconds = parseDurationSeconds(duration)
    if (seconds <= 0) return "" to ""
    return (seconds / 60).toString() to (seconds % 60).toString().padStart(2, '0')
}
