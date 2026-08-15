package com.opensetlist.app.ui.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.opensetlist.app.AppStrings

/**
 * Menu de compartilhamento de setlist, permitindo escolher o formato de exportação.
 *
 * @author ruanitto
 */
@Composable
fun SetlistShareMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onShareOpenSetlist: () -> Unit,
    onShareJustChords: () -> Unit,
    onShareJustChordsArchive: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        DropdownMenuItem(
            text = { Text(AppStrings.opensetlistFormat) },
            onClick = {
                onDismissRequest()
                onShareOpenSetlist()
            }
        )
        DropdownMenuItem(
            text = { Text(AppStrings.justChordsFormat) },
            onClick = {
                onDismissRequest()
                onShareJustChords()
            }
        )
        DropdownMenuItem(
            text = { Text(AppStrings.justChordsArchiveFormat) },
            onClick = {
                onDismissRequest()
                onShareJustChordsArchive()
            }
        )
    }
}
