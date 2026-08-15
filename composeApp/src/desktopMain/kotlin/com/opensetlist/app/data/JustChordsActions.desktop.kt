package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser

/**
 * Importação de setlist JustChords (.chopro/.jcarchive) no desktop via
 * JFileChooser, usando o nome do arquivo como nome do setlist.
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
                val chooser = JFileChooser().apply {
                    dialogTitle = "Importar setlist JustChords (.chopro/.jcarchive)"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val file = chooser.selectedFile
                    val bytes = runCatching { file.readBytes() }.getOrNull()
                    if (bytes != null) onImported(file.name, bytes)
                }
            }
        )
    }
}
