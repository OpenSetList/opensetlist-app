package com.opensetlist.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opensetlist.app.data.ChordProParser
import com.opensetlist.app.model.Song
import com.opensetlist.app.model.Tag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    song: Song,
    allTags: List<Tag>,
    initialTags: List<Tag>,
    onSave: (Song, List<String>) -> Unit,
    onNewTag: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var body by remember(song.id) { mutableStateOf(song.body) }
    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var key by remember(song.id) { mutableStateOf(song.key) }
    var tempo by remember(song.id) { mutableStateOf(song.tempo) }
    var capo by remember(song.id) { mutableStateOf(song.capo) }
    var duration by remember(song.id) { mutableStateOf(song.duration) }
    var youtubeUrl by remember(song.id) { mutableStateOf(song.youtubeUrl) }
    var selectedTagIds by remember(song.id) { mutableStateOf(initialTags.map { it.id }.toSet()) }
    var newTagText by remember { mutableStateOf("") }

    fun save() {
        val parsed = ChordProParser.parse(body)
        val updated = song.copy(
            body = body,
            title = title.trim().ifBlank { parsed.title.ifBlank { song.title } },
            artist = artist.trim().ifBlank { parsed.artist.ifBlank { song.artist } },
            key = key.trim().ifBlank { parsed.key.ifBlank { song.key } },
            tempo = tempo.trim().ifBlank { parsed.tempo.ifBlank { song.tempo } },
            capo = capo.trim().ifBlank { parsed.capo.ifBlank { song.capo } },
            duration = duration.trim().ifBlank { song.duration },
            youtubeUrl = youtubeUrl.trim()
        )
        onSave(updated, selectedTagIds.toList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Editar ${song.title}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancelar"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = ::save) {
                        Text("Salvar")
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
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Artista") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Tom") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = tempo,
                    onValueChange = { tempo = it },
                    label = { Text("BPM") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = capo,
                    onValueChange = { capo = it },
                    label = { Text("Capo") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duração") },
                    placeholder = { Text("3:45") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = { youtubeUrl = it },
                label = { Text("Link do YouTube") },
                placeholder = { Text("https://youtube.com/watch?v=...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            Text(
                text = "Tags",
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
                    placeholder = { Text("Nova tag") },
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
                        contentDescription = "Adicionar tag"
                    )
                }
            }

            Text(
                text = "Letra / Cifra (ChordPro)",
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
