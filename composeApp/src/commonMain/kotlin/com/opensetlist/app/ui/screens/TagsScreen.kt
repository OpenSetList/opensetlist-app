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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.opensetlist.app.model.Tag
import com.opensetlist.app.ui.components.SortMenu

enum class TagSort(val label: String) {
    NAME_ASC(AppStrings.sortNameAsc),
    NAME_DESC(AppStrings.sortNameDesc)
}

@Composable
fun TagsScreen(
    tags: List<Tag>,
    songCounts: Map<String, Int>,
    onTagClick: (Tag) -> Unit,
    onEdit: (Tag) -> Unit,
    onDelete: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(TagSort.NAME_ASC) }

    val filteredTags = tags.filter { tag ->
        searchQuery.isBlank() || tag.name.contains(searchQuery, ignoreCase = true)
    }

    val sortedTags = remember(filteredTags, sortOrder) {
        if (sortOrder == TagSort.NAME_ASC) {
            filteredTags.sortedBy { it.name.lowercase() }
        } else {
            filteredTags.sortedByDescending { it.name.lowercase() }
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
                    text = AppStrings.tagsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = AppStrings.tagsCount(tags.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SortMenu(
                currentLabel = sortOrder.label,
                options = TagSort.entries.map { it.label },
                onSelect = { sortOrder = TagSort.entries[it] }
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(AppStrings.searchTagsPlaceholder) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (sortedTags.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = AppStrings.noTags,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedTags, key = { it.id }) { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagClick(tag) }
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
                                text = tag.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AppStrings.songsCount(songCounts[tag.id] ?: 0),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEdit(tag) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = AppStrings.editTag
                            )
                        }
                        IconButton(onClick = { onDelete(tag) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = AppStrings.deleteTag
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
