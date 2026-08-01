package com.opensetlist.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import com.opensetlist.app.data.DatabaseDriverFactory

/**
 * Activity principal do Android, que inicia o Compose e trata arquivos abertos
 * pelo sistema (ACTION_VIEW), como arquivos .osl compartilhados.
 *
 * @author ruanitto
 */
class MainActivity : ComponentActivity() {

    private val importRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importRequest.value = readOpenIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            App(
                driverFactory = DatabaseDriverFactory(applicationContext),
                initialImport = importRequest.value
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importRequest.value = readOpenIntent(intent)
    }

    private fun readOpenIntent(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        return runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
    }
}
