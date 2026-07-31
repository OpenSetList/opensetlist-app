package com.opensetlist.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.opensetlist.app.model.Song
import com.opensetlist.app.ui.components.SortMenu

@Composable
fun FilteredSongListScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    var sortOrder by remember { mutableStateOf(SongListSort.TITLE_ASC) }

    val sortedSongs = remember(songs, sortOrder) {
        when (sortOrder) {
            SongListSort.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SongListSort.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SongListSort.ARTIST_ASC -> songs.sortedWith(
                compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
            )
            SongListSort.ARTIST_DESC -> songs.sortedWith(
                compareByDescending<Song> { it.artist.lowercase() }
                    .thenByDescending { it.title.lowercase() }
            )
            SongListSort.CREATED_ASC -> songs.sortedBy { it.sortOrder }
            SongListSort.CREATED_DESC -> songs.sortedByDescending { it.sortOrder }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            SortMenu(
                currentLabel = sortOrder.label,
                options = SongListSort.entries.map { it.label },
                onSelect = { sortOrder = SongListSort.entries[it] }
            )
        }

        if (sortedSongs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedSongs, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) }
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (song.key.isNotBlank()) {
                            Text(
                                text = song.key,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
}
