package com.opensetlist.app.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.opensetlist.app.data.db.AppDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val file = appDbFile()
        file.parentFile?.mkdirs()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        if (!file.exists() || file.length() == 0L) {
            AppDatabase.Schema.create(driver)
        } else {
            val currentVersion = driver.executeQuery(
                null,
                "PRAGMA user_version",
                { cursor ->
                    cursor.next()
                    QueryResult.Value(cursor.getLong(0) ?: 0L)
                },
                0,
                null
            ).value ?: 0L
            if (currentVersion < AppDatabase.Schema.version) {
                AppDatabase.Schema.migrate(driver, currentVersion, AppDatabase.Schema.version)
            }
        }
        return driver
    }
}

fun appDbFile(): File = File(System.getProperty("user.home"), ".opensetlist/setlist.db")
