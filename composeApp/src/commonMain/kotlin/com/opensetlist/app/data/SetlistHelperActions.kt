package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import com.opensetlist.app.model.SetlistHelperBackup

class SetlistHelperActions(
    val importBackup: () -> Unit
)

@Composable
expect fun rememberSetlistHelperActions(
    onImported: (SetlistHelperBackup?) -> Unit
): SetlistHelperActions
