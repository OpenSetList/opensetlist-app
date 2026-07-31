package com.opensetlist.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.opensetlist.app.data.db.AppDatabase

/**
 * Fábrica de driver SQLDelight para iOS (SQLite nativo).
 *
 * @author ruanitto
 */
actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = "setlist.db"
        )
    }
}
