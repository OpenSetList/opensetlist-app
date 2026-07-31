package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import com.opensetlist.app.model.BackupData

/**
 * Ações de importação e exportação de backup por plataforma.
 *
 * @author ruanitto
 */
class BackupActions(
    val importBackup: () -> Unit,
    val exportBytes: () -> ByteArray?
)

/**
 * Cria as ações de backup conforme a plataforma atual.
 *
 * @author ruanitto
 */
@Composable
expect fun rememberBackupActions(
    onImported: (BackupData?) -> Unit
): BackupActions
