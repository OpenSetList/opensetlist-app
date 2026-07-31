package com.opensetlist.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.opensetlist.app.model.Artist
import com.opensetlist.app.ui.components.SortMenu

enum class ArtistSort(val label: String) {
    NAME_ASC("Nome (A-Z)"),
    NAME_DESC("Nome (Z-A)")
}

@Composable
fun ArtistsScreen(
    artists: List<Artist>,
    songCounts: Map<String, Int>,
    onArtistClick: (Artist) -> Unit,
    onEdit: (Artist) -> Unit,
    onDelete: (Artist) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOrder by remember { mutableStateOf(ArtistSort.NAME_ASC) }

    val sortedArtists = remember(artists, sortOrder) {
        if (sortOrder == ArtistSort.NAME_ASC) {
            artists.sortedBy { it.name.lowercase() }
        } else {
            artists.sortedByDescending { it.name.lowercase() }
        }
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
                    text = "Artistas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${artists.size} artistas",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SortMenu(
                currentLabel = sortOrder.label,
                options = ArtistSort.entries.map { it.label },
                onSelect = { sortOrder = ArtistSort.entries[it] }
            )
        }

        if (sortedArtists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum artista cadastrado",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedArtists, key = { it.id }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onArtistClick(artist) }
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${songCounts[artist.name] ?: 0} músicas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEdit(artist) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar artista"
                            )
                        }
                        IconButton(onClick = { onDelete(artist) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir artista"
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
