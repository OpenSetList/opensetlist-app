package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.opensetlist.app.model.SetlistHelperBackup

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
