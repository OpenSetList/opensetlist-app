package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import javax.swing.JFileChooser

/**
 * Importação de setlist JustChords (.chopro) no desktop via JFileChooser,
 * usando o nome do arquivo como nome do setlist.
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
                val chooser = JFileChooser().apply {
                    dialogTitle = "Importar setlist JustChords (.chopro)"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val file = chooser.selectedFile
                    val content = runCatching { file.readText() }.getOrNull()
                    if (content != null) onImported(file.name, content)
                }
            }
        )
    }
}
