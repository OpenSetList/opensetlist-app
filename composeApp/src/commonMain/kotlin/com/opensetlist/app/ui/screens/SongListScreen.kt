package com.opensetlist.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.Song
import com.opensetlist.app.ui.components.SortMenu

enum class SongListSort(val label: String) {
    TITLE_ASC(AppStrings.sortNameAsc),
    TITLE_DESC(AppStrings.sortNameDesc),
    ARTIST_ASC(AppStrings.sortArtistAsc),
    ARTIST_DESC(AppStrings.sortArtistDesc),
    CREATED_ASC(AppStrings.sortCreatedAsc),
    CREATED_DESC(AppStrings.sortCreatedDesc)
}

@Composable
fun SongListScreen(
    songs: List<Song>,
    setlists: List<Setlist>,
    onSongClick: (Song) -> Unit,
    onSetlistClick: (Setlist) -> Unit,
    onNewSong: () -> Unit,
    onDeleteSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SongListSort.TITLE_ASC) }

    val filteredSongs = songs.filter { song ->
        searchQuery.isBlank() ||
        song.title.contains(searchQuery, ignoreCase = true) ||
        song.artist.contains(searchQuery, ignoreCase = true)
    }

    val sortedSongs = remember(filteredSongs, sortOrder) {
        when (sortOrder) {
            SongListSort.TITLE_ASC -> filteredSongs.sortedBy { it.title.lowercase() }
            SongListSort.TITLE_DESC -> filteredSongs.sortedByDescending { it.title.lowercase() }
            SongListSort.ARTIST_ASC -> filteredSongs.sortedWith(
                compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
            )
            SongListSort.ARTIST_DESC -> filteredSongs.sortedWith(
                compareByDescending<Song> { it.artist.lowercase() }
                    .thenByDescending { it.title.lowercase() }
            )
            SongListSort.CREATED_ASC -> filteredSongs.sortedBy { it.sortOrder }
            SongListSort.CREATED_DESC -> filteredSongs.sortedByDescending { it.sortOrder }
        }
    }

    val filteredSetlists = if (searchQuery.isBlank()) emptyList() else setlists.filter { setlist ->
        setlist.name.contains(searchQuery, ignoreCase = true)
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
                    text = AppStrings.allSongsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = AppStrings.songsCount(songs.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SortMenu(
                currentLabel = sortOrder.label,
                options = SongListSort.entries.map { it.label },
                onSelect = { sortOrder = SongListSort.entries[it] }
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(AppStrings.searchSongsPlaceholder) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (filteredSetlists.isNotEmpty()) {
                        item(key = "setlists_header") {
                            Text(
                                text = AppStrings.setlistsHeader,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(filteredSetlists, key = { "setlist_${it.id}" }) { setlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSetlistClick(setlist) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                        text = setlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = AppStrings.songsCount(setlist.songs.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        item(key = "songs_header") {
                            Text(
                                text = AppStrings.songsHeader,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }

                    items(sortedSongs, key = { it.id }) { song ->
                        SongListItem(
                            song = song,
                            onClick = { onSongClick(song) },
                            onDelete = { onDeleteSong(song) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onNewSong,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = AppStrings.newSong
                )
            }
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (song.key.isNotBlank()) {
            Text(
                text = song.key,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = AppStrings.deleteSong,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
