package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Importação de setlist JustChords (.chopro) no iOS (ainda não suportada).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberJustChordsActions(
    onImported: (fileName: String, content: String) -> Unit
): JustChordsActions {
    return remember {
        JustChordsActions(
            importFile = {
                onImported("", "")
            }
        )
    }
}
