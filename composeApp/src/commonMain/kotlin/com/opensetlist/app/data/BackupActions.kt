package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import com.opensetlist.app.model.BackupData

class BackupActions(
    val importBackup: () -> Unit,
    val exportBytes: () -> ByteArray?
)

@Composable
expect fun rememberBackupActions(
    onImported: (BackupData?) -> Unit
): BackupActions
