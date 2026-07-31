package com.opensetlist.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.opensetlist.app.data.DatabaseDriverFactory

/**
 * Ponto de entrada da aplicação desktop.
 *
 * @author ruanitto
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = AppStrings.appName
    ) {
        App(DatabaseDriverFactory())
    }
}
