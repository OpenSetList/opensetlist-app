package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser

/**
 * Ações de arquivo no desktop (diálogos nativos via JFileChooser e Desktop).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberFileActions(
    getExportContent: () -> String?,
    onImported: (String) -> Unit,
    onExported: (Boolean) -> Unit,
    onShared: (Boolean) -> Unit,
    getExportBytes: () -> ByteArray?
): FileActions {
    val currentContent = rememberUpdatedState(getExportContent)
    val currentBytes = rememberUpdatedState(getExportBytes)

    return remember {
        FileActions(
            importFile = {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Importar"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val content = runCatching { chooser.selectedFile.readText() }.getOrNull()
                    if (content != null) onImported(content)
                }
            },
            saveFile = { fileName, _ ->
                val chooser = JFileChooser().apply {
                    dialogTitle = "Salvar"
                    selectedFile = File(sanitizeFileName(fileName))
                }
                if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val target = chooser.selectedFile
                    val ok = runCatching {
                        val bytes = currentBytes.value()
                        when {
                            bytes != null -> target.writeBytes(bytes)
                            else -> {
                                val content = currentContent.value() ?: return@runCatching false
                                target.writeText(content)
                            }
                        }
                        true
                    }.getOrDefault(false)
                    onExported(ok)
                }
            },
            shareFile = { fileName, _ ->
                val ok = runCatching {
                    val dir = sharedDir()
                    val target = File(dir, sanitizeFileName(fileName))
                    val bytes = currentBytes.value()
                    when {
                        bytes != null -> target.writeBytes(bytes)
                        else -> {
                            val content = currentContent.value() ?: return@runCatching false
                            target.writeText(content)
                        }
                    }
                    true
                }.getOrDefault(false)
                onShared(ok)
            },
            openUrl = { url ->
                runCatching {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    }
                }
            },
            saveProBatch = { files, onProgress, isCancelled ->
                val chooser = JFileChooser().apply {
                    dialogTitle = "Exportar músicas (.pro)"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    val dir = chooser.selectedFile
                    var cancelled = false
                    for ((fileName, content) in files) {
                        if (isCancelled()) {
                            cancelled = true
                            break
                        }
                        onProgress(fileName, ProBatchEvent.START)
                        val ok = runCatching {
                            File(dir, sanitizeFileName(fileName)).writeText(content)
                            true
                        }.getOrDefault(false)
                        onProgress(
                            fileName,
                            if (ok) ProBatchEvent.DONE else ProBatchEvent.FAILED
                        )
                    }
                    onProgress(
                        "",
                        if (cancelled) ProBatchEvent.CANCELLED else ProBatchEvent.COMPLETED
                    )
                } else {
                    onProgress("", ProBatchEvent.CANCELLED)
                }
            }
        )
    }
}

private fun sharedDir(): File =
    File(System.getProperty("user.home"), ".opensetlist/shared").apply { mkdirs() }

private fun sanitizeFileName(name: String): String =
    name.replace(Regex("[^\\p{L}\\p{N}._\\- ]"), "_")
