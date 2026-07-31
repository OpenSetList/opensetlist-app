package com.opensetlist.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberFileActions(
    getExportContent: () -> String?,
    onImported: (String) -> Unit,
    onExported: (Boolean) -> Unit,
    onShared: (Boolean) -> Unit,
    getExportBytes: () -> ByteArray?
): FileActions {
    val context = LocalContext.current
    val currentContent = rememberUpdatedState(getExportContent)
    val currentBytes = rememberUpdatedState(getExportBytes)

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }
            if (content != null) onImported(content)
        }
    }

    val saveTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            val content = currentContent.value()
            val ok = content != null && runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(content.toByteArray())
                }
            }.isSuccess
            onExported(ok)
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            val content = currentContent.value()
            val ok = content != null && runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(content.toByteArray())
                }
            }.isSuccess
            onExported(ok)
        }
    }

    val saveBinaryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = runCatching {
                val bytes = currentBytes.value()
                when {
                    bytes != null ->
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
                    else -> {
                        val content = currentContent.value() ?: return@runCatching false
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(content.toByteArray())
                        } != null
                    }
                }
            }.getOrDefault(false)
            onExported(ok)
        }
    }

    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onShared(true)
    }

    fun shareFile(fileName: String, mimeType: String) {
        val bytes = currentBytes.value()
        if (bytes != null) {
            val file = writeSharedFileBytes(context, fileName, bytes)
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                shareLauncher.launch(Intent.createChooser(intent, "Compartilhar"))
            }
            return
        }
        val content = currentContent.value() ?: return
        val file = writeSharedFile(context, fileName, content)
        if (file != null) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareLauncher.launch(Intent.createChooser(intent, "Compartilhar"))
        }
    }

    return remember {
        FileActions(
            importFile = {
                importLauncher.launch(
                    arrayOf(
                        "application/json",
                        "text/plain",
                        "application/octet-stream",
                        "application/x-chordpro"
                    )
                )
            },
            saveFile = { fileName, mimeType ->
                if (mimeType == "application/json") saveJsonLauncher.launch(fileName)
                else if (mimeType == "application/octet-stream") saveBinaryLauncher.launch(fileName)
                else saveTextLauncher.launch(fileName)
            },
            shareFile = ::shareFile,
            openUrl = { url ->
                runCatching {
                    val uri = Uri.parse(url)
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, uri)
                    )
                }
            }
        )
    }
}

private fun writeSharedFileBytes(context: Context, fileName: String, bytes: ByteArray): File? {
    return runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, sanitizeFileName(fileName))
        file.writeBytes(bytes)
        file
    }.getOrNull()
}

private fun writeSharedFile(context: Context, fileName: String, content: String): File? {
    return runCatching {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, sanitizeFileName(fileName))
        file.writeText(content)
        file
    }.getOrNull()
}

private fun sanitizeFileName(name: String): String =
    name.replace(Regex("[^A-Za-z0-9._\\- ]"), "_")
