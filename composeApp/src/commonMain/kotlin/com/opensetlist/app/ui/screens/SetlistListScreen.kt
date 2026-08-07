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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
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
import com.opensetlist.app.data.formatDuration
import com.opensetlist.app.data.formatEpochDateTime
import com.opensetlist.app.data.parseDurationSeconds
import com.opensetlist.app.model.Setlist
import com.opensetlist.app.ui.components.SortMenu

/** Critérios de ordenação da lista de setlists. */
enum class SetlistSort(val label: String) {
    NAME_ASC(AppStrings.sortNameAsc),
    NAME_DESC(AppStrings.sortNameDesc),
    LOCATION_ASC(AppStrings.sortLocationAsc),
    LOCATION_DESC(AppStrings.sortLocationDesc),
    DATE_ASC(AppStrings.sortDateAsc),
    DATE_DESC(AppStrings.sortDateDesc),
    SONGS_ASC(AppStrings.sortSongsAsc),
    SONGS_DESC(AppStrings.sortSongsDesc)
}

/**
 * Tela de listagem de setlists, com compartilhamento e exclusão.
 *
 * @author ruanitto
 */
@Composable
fun SetlistListScreen(
    setlists: List<Setlist>,
    onSetlistClick: (Setlist) -> Unit,
    onShare: (Setlist) -> Unit,
    onEdit: (Setlist) -> Unit,
    onDelete: (Setlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var sortOrder by remember { mutableStateOf(SetlistSort.DATE_DESC) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSetlists = setlists.filter { setlist ->
        searchQuery.isBlank() ||
        setlist.name.contains(searchQuery, ignoreCase = true) ||
        setlist.location.contains(searchQuery, ignoreCase = true)
    }

    val sortedSetlists = remember(filteredSetlists, sortOrder) {
        when (sortOrder) {
            SetlistSort.NAME_ASC -> filteredSetlists.sortedBy { it.name.lowercase() }
            SetlistSort.NAME_DESC -> filteredSetlists.sortedByDescending { it.name.lowercase() }
            SetlistSort.LOCATION_ASC -> filteredSetlists.sortedWith(
                compareBy({ it.location.isBlank() }, { it.location.lowercase() })
            )
            SetlistSort.LOCATION_DESC -> filteredSetlists.sortedWith(
                compareBy<Setlist> { it.location.isBlank() }
                    .thenByDescending { it.location.lowercase() }
            )
            SetlistSort.DATE_ASC -> filteredSetlists.sortedBy { dateSortKey(it) }
            SetlistSort.DATE_DESC -> filteredSetlists.sortedWith(
                compareBy<Setlist> { it.date <= 0L }
                    .thenByDescending { dateSortKey(it) }
            )
            SetlistSort.SONGS_ASC -> filteredSetlists.sortedBy { it.songs.size }
            SetlistSort.SONGS_DESC -> filteredSetlists.sortedByDescending { it.songs.size }
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
                    text = AppStrings.setlistsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = AppStrings.setlistsCount(setlists.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SortMenu(
                currentLabel = sortOrder.label,
                options = SetlistSort.entries.map { it.label },
                onSelect = { sortOrder = SetlistSort.entries[it] }
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(AppStrings.searchSetlistsPlaceholder) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (sortedSetlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isBlank()) AppStrings.noSetlistsYet
                    else AppStrings.noSetlistsFound,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedSetlists, key = { it.id }) { setlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetlistClick(setlist) }
                            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = setlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    append(AppStrings.songsCount(setlist.songs.size))
                                    val total = setlist.songs.sumOf {
                                        parseDurationSeconds(it.duration)
                                    }
                                    val formatted = formatDuration(total)
                                    if (formatted.isNotEmpty()) append(" • $formatted")
                                    if (setlist.location.isNotBlank()) {
                                        append(" • ${setlist.location}")
                                    }
                                    if (setlist.date > 0) {
                                        append(" • ${formatEpochDateTime(setlist.date)}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onShare(setlist) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = AppStrings.shareSetlist
                                )
                            }
                            IconButton(onClick = { onEdit(setlist) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = AppStrings.editSetlist
                                )
                            }
                            IconButton(onClick = { onDelete(setlist) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = AppStrings.deleteSetlist
                                )
                            }
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

private fun dateSortKey(setlist: Setlist): Long =
    if (setlist.date > 0) setlist.date else Long.MAX_VALUE
