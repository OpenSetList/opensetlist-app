package com.opensetlist.app.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.opensetlist.app.data.db.AppDatabase

actual class DatabaseDriverFactory(private val context: Context) {

    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AppDatabase.Schema,
            context = context,
            name = "setlist.db"
        )
    }
}
