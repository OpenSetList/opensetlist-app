package com.opensetlist.app.data

import androidx.compose.runtime.Composable

class FileActions(
    val importFile: () -> Unit,
    val saveFile: (fileName: String, mimeType: String) -> Unit,
    val shareFile: (fileName: String, mimeType: String) -> Unit,
    val openUrl: (String) -> Unit
)

@Composable
expect fun rememberFileActions(
    getExportContent: () -> String?,
    onImported: (String) -> Unit,
    onExported: (Boolean) -> Unit,
    onShared: (Boolean) -> Unit,
    getExportBytes: () -> ByteArray?
): FileActions
