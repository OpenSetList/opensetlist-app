package com.opensetlist.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.opensetlist.app.data.DatabaseDriverFactory

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = AppStrings.appName
    ) {
        App(DatabaseDriverFactory())
    }
}
