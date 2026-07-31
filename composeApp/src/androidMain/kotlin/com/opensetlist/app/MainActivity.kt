package com.opensetlist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.opensetlist.app.data.DatabaseDriverFactory

/**
 * Activity principal do Android, que inicia o Compose.
 *
 * @author ruanitto
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            App(DatabaseDriverFactory(applicationContext))
        }
    }
}
