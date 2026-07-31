package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.opensetlist.app.model.BackupData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun readDatabaseBytes(): ByteArray? {
    val dir = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).firstOrNull() as? String ?: return null
    val path = "$dir/setlist.db"
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    return ByteArray(data.length.toInt()).apply {
        usePinned { pinned ->
            data.getBytes(pinned.addressOf(0), data.length)
        }
    }
}

@Composable
actual fun rememberBackupActions(
    onImported: (BackupData?) -> Unit
): BackupActions {
    return remember {
        BackupActions(
            importBackup = { onImported(null) },
            exportBytes = { readDatabaseBytes() }
        )
    }
}
