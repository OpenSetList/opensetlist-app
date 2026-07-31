package com.opensetlist.app.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.opensetlist.app.data.db.AppDatabase
import java.io.File

/**
 * Fábrica de driver SQLDelight para desktop (JDBC SQLite).
 *
 * @author ruanitto
 */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val file = appDbFile()
        file.parentFile?.mkdirs()
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${file.absolutePath}",
            schema = AppDatabase.Schema
        )
    }
}

/** Caminho do banco local no diretório do usuário. */
fun appDbFile(): File = File(System.getProperty("user.home"), ".opensetlist/setlist.db")
