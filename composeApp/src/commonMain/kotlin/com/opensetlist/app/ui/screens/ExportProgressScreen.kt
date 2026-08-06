package com.opensetlist.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opensetlist.app.AppStrings
import com.opensetlist.app.model.ExportLogEntry
import com.opensetlist.app.model.ExportLogKind

/**
 * Tela que mostra o progresso da exportação em lote de músicas (.pro),
 * com log por música e tempo total de execução.
 *
 * @author ruanitto
 */
@Composable
fun ExportProgressScreen(
    entries: List<ExportLogEntry>,
    running: Boolean,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.lastIndex)
        }
    }

    val started = entries.count { it.kind == ExportLogKind.START }
    val finished = entries.count {
        it.kind == ExportLogKind.DONE || it.kind == ExportLogKind.FAILED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (running) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (running) AppStrings.proExportRunning else AppStrings.proExportFinished,
                style = MaterialTheme.typography.titleMedium,
                color = if (running) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            if (started > 0) {
                Text(
                    text = "$finished/$started",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(entries) { entry ->
                    ExportLogLine(entry)
                }
            }
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(AppStrings.proExportClose)
        }
    }
}

@Composable
private fun ExportLogLine(entry: ExportLogEntry) {
    val color = when (entry.kind) {
        ExportLogKind.FAILED -> MaterialTheme.colorScheme.error
        ExportLogKind.SUCCESS -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = entry.text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
