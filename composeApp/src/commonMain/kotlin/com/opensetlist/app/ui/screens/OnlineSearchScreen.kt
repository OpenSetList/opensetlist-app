package com.opensetlist.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.data.ChordProParser
import com.opensetlist.app.data.CifraClub
import com.opensetlist.app.data.CifraSearchOutcome
import com.opensetlist.app.data.CifraSheet
import com.opensetlist.app.data.CifraSong
import com.opensetlist.app.data.UltimateGuitar
import com.opensetlist.app.data.toImportBody
import com.opensetlist.app.ui.components.ChordProView
import kotlinx.coroutines.launch

/** Fonte da busca online. */
enum class OnlineSearchSource(val label: String) {
    CIFRA_CLUB(AppStrings.cifraClubSource),
    ULTIMATE_GUITAR(AppStrings.ultimateGuitarSource),
    GOOGLE(AppStrings.googleSource)
}

/**
 * Tela de busca de cifras na internet (Cifra Club, Ultimate Guitar ou Google).
 */
@Composable
fun OnlineSearchScreen(
    fallbackArtist: String = "",
    fallbackTitle: String = "",
    onImportSheet: (CifraSheet) -> Unit,
    onOpenInEditor: (CifraSheet) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(OnlineSearchSource.CIFRA_CLUB) }
    var loading by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<CifraSheet?>(null) }
    var songs by remember { mutableStateOf<List<CifraSong>?>(null) }
    var loadingSheet by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun doSearch() {
        val term = query.trim()
        if (term.isBlank()) return
        when (source) {
            OnlineSearchSource.GOOGLE -> {
                val googleQuery = if (term.contains("chords", ignoreCase = true)) term
                else "$term chords"
                onOpenUrl(CifraClub.googleSearchUrl(googleQuery))
            }
            else -> {
                loading = true
                sheet = null
                songs = null
                message = null
                scope.launch {
                    val outcome = if (source == OnlineSearchSource.ULTIMATE_GUITAR) {
                        UltimateGuitar.search(term, fallbackArtist, fallbackTitle)
                    } else {
                        CifraClub.search(term, fallbackArtist, fallbackTitle)
                    }
                    loading = false
                    when (outcome) {
                        is CifraSearchOutcome.Sheet -> sheet = outcome.sheet
                        is CifraSearchOutcome.Songs -> songs = outcome.songs
                        is CifraSearchOutcome.NoResult -> message = AppStrings.noCifraFound
                    }
                }
            }
        }
    }

    fun openSong(song: CifraSong) {
        if (loadingSheet != null) return
        loadingSheet = song.url
        message = null
        scope.launch {
            val result = if (source == OnlineSearchSource.ULTIMATE_GUITAR) {
                UltimateGuitar.fetchSongByUrl(song.url)
            } else {
                CifraClub.fetchSongByUrl(song.url)
            }
            loadingSheet = null
            if (result != null) sheet = result
            else message = AppStrings.onlineSearchFailed
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(AppStrings.onlineSearchPlaceholder) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { doSearch() }),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OnlineSearchSource.entries.forEach { option ->
                FilterChip(
                    selected = source == option,
                    onClick = { source = option },
                    label = { Text(option.label) }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Button(onClick = { doSearch() }) {
                    Text(AppStrings.onlineSearchAction)
                }
            }
        }

        when {
            message != null -> {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            loadingSheet != null -> {
                Text(
                    text = AppStrings.searchRunning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            sheet != null -> {
                PreviewCard(
                    sheet = sheet!!,
                    onImport = { onImportSheet(sheet!!) },
                    onOpenInEditor = { onOpenInEditor(sheet!!) },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                )
            }

            songs != null -> {
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(songs.orEmpty(), key = { it.url }) { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = { openSong(song) }
                            ) {
                                Text(AppStrings.importChordSheet)
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCard(
    sheet: CifraSheet,
    onImport: () -> Unit,
    onOpenInEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsed = remember(sheet.body) { ChordProParser.parse(sheet.toImportBody()) }

    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = sheet.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sheet.artist,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val meta = buildList {
                if (sheet.key.isNotBlank()) add("${AppStrings.keyLabel}: ${sheet.key}")
                if (sheet.capo.isNotBlank()) add("${AppStrings.capoLabel}: ${sheet.capo}")
                if (sheet.tuning.isNotBlank()) add("${AppStrings.tuningLabel}: ${sheet.tuning}")
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        ChordProView(
            song = parsed,
            tags = emptyList(),
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onImport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text(AppStrings.importChordSheet, modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onOpenInEditor, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Text(AppStrings.openInEditor, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}