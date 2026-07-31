package com.opensetlist.app

import androidx.compose.ui.window.ComposeUIViewController
import com.opensetlist.app.data.DatabaseDriverFactory

fun MainViewController() = ComposeUIViewController { App(DatabaseDriverFactory()) }
