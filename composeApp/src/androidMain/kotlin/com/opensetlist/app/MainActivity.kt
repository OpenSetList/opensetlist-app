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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.opensetlist.app.data.DatabaseDriverFactory
import kotlinx.coroutines.delay

/**
 * Activity principal do Android, que inicia o Compose e trata arquivos abertos
 * pelo sistema (ACTION_VIEW), como arquivos .osl compartilhados, .chopro e
 * .jcarchive do JustChords (importados como setlist), além de links de cifras
 * enviados por outros apps (ACTION_SEND), que são importados automaticamente.
 *
 * @author ruanitto
 */
class MainActivity : ComponentActivity() {

    private data class OpenedFile(val name: String?, val bytes: ByteArray)

    private val importRequest = mutableStateOf<OpenedFile?>(null)
    private val sharedLinkRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        importRequest.value = readOpenIntent(intent)
        sharedLinkRequest.value = readShareLink(intent)
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
                        .background(Color(0xFFFDFDFD)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.splash_logo),
                        contentDescription = AppStrings.appName,
                        modifier = Modifier.size(240.dp)
                    )
                    Text(
                        text = AppStrings.splashDevelopedBy,
                        color = Color(0xFF1B5A89),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp)
                    )
                }
            } else {
                App(
                    driverFactory = DatabaseDriverFactory(applicationContext),
                    initialImportFileName = importRequest.value?.name,
                    initialImportBytes = importRequest.value?.bytes,
                    onInitialImportConsumed = { importRequest.value = null },
                    initialSharedLink = sharedLinkRequest.value,
                    onInitialSharedConsumed = { sharedLinkRequest.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importRequest.value = readOpenIntent(intent)
        sharedLinkRequest.value = readShareLink(intent)
    }

    private fun readShareLink(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            .split(Regex("\\s+"))
        return text.firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
            ?.trimEnd('.', ',', ';', ':', ')', '(', '"', '\'', '!', '?')
    }

    private fun readOpenIntent(intent: Intent?): OpenedFile? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val bytes = runCatching {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        val name = queryDisplayName(uri) ?: uri.lastPathSegment?.let { Uri.decode(it) }
        return OpenedFile(name = name, bytes = bytes)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
    }
}
