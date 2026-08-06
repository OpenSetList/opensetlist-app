package com.opensetlist.app.data

import androidx.compose.runtime.Composable

/**
 * Ações de importação, exportação e compartilhamento de arquivos por plataforma.
 *
 * @author ruanitto
 */
class FileActions(
    val importFile: () -> Unit,
    val saveFile: (fileName: String, mimeType: String) -> Unit,
    val shareFile: (fileName: String, mimeType: String) -> Unit,
    val openUrl: (String) -> Unit,
    val saveProBatch: (List<Pair<String, String>>) -> Unit
)

/**
 * Cria ações de arquivo conforme a plataforma atual.
 *
 * @author ruanitto
 */
@Composable
expect fun rememberFileActions(
    getExportContent: () -> String?,
    onImported: (String) -> Unit,
    onExported: (Boolean) -> Unit,
    onShared: (Boolean) -> Unit,
    getExportBytes: () -> ByteArray?,
    onProBatchExported: (saved: Int, failed: Int) -> Unit
): FileActions
