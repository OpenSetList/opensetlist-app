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
            title = "Backup do banco de dados",
            icon = Icons.Default.Backup
        ) {
            SettingsRow(
                label = "Exportar backup completo (.db)",
                subtitle = "Copia o banco SQLite inteiro, pronto para uso externo",
                actions = {
                    IconButton(onClick = { onExportBackup(false) }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Salvar backup em arquivo"
                        )
                    }
                    IconButton(onClick = { onExportBackup(true) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar backup"
                        )
                    }
                }
            )
            SettingsRow(
                label = "Importar backup (.db)",
                subtitle = "Restaura todos os dados a partir de um arquivo .db",
                actions = {
                    IconButton(onClick = onImportBackup) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Importar backup"
                        )
                    }
                }
            )
        }

        SettingsSection(
            title = "Músicas",
            icon = Icons.Default.LibraryMusic
        ) {
            SettingsRow(
                label = "Exportar todas as músicas",
                actions = {
                    IconButton(onClick = { onExportAllSongs(false) }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Salvar músicas em arquivo"
                        )
                    }
                    IconButton(onClick = { onExportAllSongs(true) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar músicas"
                        )
                    }
                }
            )
            SettingsRow(
                label = "Importar músicas (em lote)",
                actions = {
                    IconButton(onClick = onImportSongs) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Importar músicas"
                        )
                    }
                }
            )
        }

        SettingsSection(
            title = "SetList Helper",
            icon = Icons.Default.MusicNote
        ) {
            SettingsRow(
                label = "Importar backup do SetList Helper",
                subtitle = "Restaura músicas e setlists de um backup .db",
                actions = {
                    IconButton(onClick = onImportSetlistHelper) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Importar backup do SetList Helper"
                        )
                    }
                }
            )
        }

        SettingsSection(
            title = "Setlists",
            icon = Icons.AutoMirrored.Filled.List
        ) {
            SettingsRow(
                label = "Importar setlist compartilhada",
                actions = {
                    IconButton(onClick = onImportSet) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Importar setlist"
                        )
                    }
                }
            )

            if (setlists.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nenhuma setlist criada ainda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                setlists.forEach { setlist ->
                    SettingsRow(
                        label = setlist.name,
                        subtitle = "${setlist.songs.size} músicas",
                        actions = {
                            IconButton(onClick = { onShareSetlist(setlist) }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartilhar ${setlist.name}"
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
