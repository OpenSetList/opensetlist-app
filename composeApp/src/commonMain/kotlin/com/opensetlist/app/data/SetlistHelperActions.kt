package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import com.opensetlist.app.model.SetlistHelperBackup

/**
 * Ações de importação de backup do Setlist Helper por plataforma.
 *
 * @author ruanitto
 */
class SetlistHelperActions(
    val importBackup: () -> Unit
)

/**
 * Cria as ações de backup do Setlist Helper conforme a plataforma atual.
 *
 * @author ruanitto
 */
@Composable
expect fun rememberSetlistHelperActions(
    onImported: (SetlistHelperBackup?) -> Unit
): SetlistHelperActions
