package com.opensetlist.app.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Nome da versão do app no Android, lido do PackageManager.
 *
 * @author ruanitto
 */
@Composable
actual fun appVersionName(): String {
    val context = LocalContext.current
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }
}
