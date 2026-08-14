package com.opensetlist.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.opensetlist.app.data.DatabaseDriverFactory
import kotlinx.coroutines.delay

/**
 * Activity principal do Android, que inicia o Compose e trata arquivos abertos
 * pelo sistema (ACTION_VIEW), como arquivos .osl compartilhados e .chopro do
 * JustChords (importados como setlist).
 *
 * @author ruanitto
 */
class MainActivity : ComponentActivity() {

    private data class OpenedFile(val name: String?, val content: String)

    private val importRequest = mutableStateOf<OpenedFile?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        importRequest.value = readOpenIntent(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(900L)
                showSplash = false
            }
            if (showSplash) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null
                    )
                }
            } else {
                App(
                    driverFactory = DatabaseDriverFactory(applicationContext),
                    initialImport = importRequest.value?.content,
                    initialImportFileName = importRequest.value?.name
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importRequest.value = readOpenIntent(intent)
    }

    private fun readOpenIntent(intent: Intent?): OpenedFile? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val content = runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return null
        val name = queryDisplayName(uri) ?: uri.lastPathSegment?.let { Uri.decode(it) }
        return OpenedFile(name = name, content = content)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
    }
}
