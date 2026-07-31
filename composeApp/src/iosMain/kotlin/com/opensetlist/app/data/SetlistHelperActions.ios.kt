package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.opensetlist.app.model.SetlistHelperBackup

/**
 * Importação de backup do SetList Helper no iOS (ainda não suportada).
 *
 * @author ruanitto
 */
@Composable
actual fun rememberSetlistHelperActions(
    onImported: (SetlistHelperBackup?) -> Unit
): SetlistHelperActions {
    return remember {
        SetlistHelperActions(
            importBackup = {
                onImported(null)
            }
        )
    }
}
