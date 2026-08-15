package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Importação de setlist JustChords (.chopro/.jcarchive) no iOS (ainda não suportada).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberJustChordsActions(
    onImported: (fileName: String, bytes: ByteArray) -> Unit
): JustChordsActions {
    return remember {
        JustChordsActions(
            importFile = {
                onImported("", ByteArray(0))
            }
        )
    }
}
