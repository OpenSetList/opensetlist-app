package com.opensetlist.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.Setlist

@Composable
fun SettingsScreen(
    setlists: List<Setlist>,
    onExportBackup: (share: Boolean) -> Unit,
    onImportBackup: () -> Unit,
    onExportAllSongs: (share: Boolean) -> Unit,
    onImportSongs: () -> Unit,
    onImportSet: () -> Unit,
    onShareSetlist: (Setlist) -> Unit,
    onImportSetlistHelper: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSection(
            title = AppStrings.backupDbTitle,
            icon = Icons.Default.Backup
        ) {
            SettingsRow(
                label = AppStrings.exportFullBackup,
                subtitle = AppStrings.exportFullBackupSubtitle,
                actions = {
                    IconButton(onClick = { onExportBackup(false) }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = AppStrings.saveBackupToFile
                        )
                    }
                    IconButton(onClick = { onExportBackup(true) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = AppStrings.shareBackup
                        )
                    }
                }
            )
            SettingsRow(
                label = AppStrings.importBackup,
                subtitle = AppStrings.importBackupSubtitle,
                actions = {
                    IconButton(onClick = onImportBackup) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = AppStrings.importBackupFile
                        )
                    }
                }
            )
        }

        SettingsSection(
            title = AppStrings.songsSectionTitle,
            icon = Icons.Default.LibraryMusic
        ) {
            SettingsRow(
                label = AppStrings.exportAllSongs,
                actions = {
                    IconButton(onClick = { onExportAllSongs(false) }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = AppStrings.saveSongsToFile
                        )
                    }
                    IconButton(onClick = { onExportAllSongs(true) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = AppStrings.shareSongs
                        )
                    }
                }
            )
            SettingsRow(
                label = AppStrings.importSongsBatch,
                actions = {
                    IconButton(onClick = onImportSongs) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = AppStrings.importSongs
                        )
                    }
                }
            )
        }

        SettingsSection(
            title = AppStrings.setlistHelperTitle,
            icon = Icons.Default.MusicNote
        ) {
            SettingsRow(
                label = AppStrings.importSetlistHelper,
                subtitle = AppStrings.importSetlistHelperSubtitle,
                actions = {
                    IconButton(onClick = onImportSetlistHelper) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = AppStrings.importSetlistHelperFile
                        )
                    }
                }
            )
        }

        SettingsSection(
            title = AppStrings.setlistsTitle,
            icon = Icons.AutoMirrored.Filled.List
        ) {
            SettingsRow(
                label = AppStrings.importSharedSetlist,
                actions = {
                    IconButton(onClick = onImportSet) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = AppStrings.importSetlist
                        )
                    }
                }
            )

            if (setlists.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = AppStrings.noSetlistsCreatedYet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                setlists.forEach { setlist ->
                    SettingsRow(
                        label = setlist.name,
                        subtitle = AppStrings.songsCount(setlist.songs.size),
                        actions = {
                            IconButton(onClick = { onShareSetlist(setlist) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = AppStrings.shareSetlistName(setlist.name)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    actions: @Composable () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions()
        }
    }
}
