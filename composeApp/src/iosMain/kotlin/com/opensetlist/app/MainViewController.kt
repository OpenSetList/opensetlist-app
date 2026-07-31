package com.opensetlist.app

import androidx.compose.ui.window.ComposeUIViewController
import com.opensetlist.app.data.DatabaseDriverFactory

/**
 * ViewController raiz do iOS, que inicia o Compose.
 *
 * @author ruanitto
 */
fun MainViewController() = ComposeUIViewController { App(DatabaseDriverFactory()) }
