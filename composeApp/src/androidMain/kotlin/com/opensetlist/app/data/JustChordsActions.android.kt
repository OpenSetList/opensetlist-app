package com.opensetlist.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Importação de setlist JustChords (.chopro/.jcarchive) no Android via SAF,
 * lendo o nome do arquivo para usar como nome do setlist.
 *
 * @author ruanitto
 */
@Composable
actual fun rememberJustChordsActions(
    onImported: (fileName: String, bytes: ByteArray) -> Unit
): JustChordsActions {
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(context, uri) ?: "setlist.${JustChords.FILE_EXTENSION}"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) onImported(name, bytes)
        }
    }

    return remember {
        JustChordsActions(
            importFile = {
                importLauncher.launch(
                    arrayOf(
                        "text/plain",
                        "application/x-chordpro",
                        "application/zip",
                        "application/x-zip-compressed",
                        "application/octet-stream"
                    )
                )
            }
        )
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}
