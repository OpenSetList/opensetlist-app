package com.opensetlist.app.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Fábrica do driver SQLDelight de cada plataforma.
 *
 * @author ruanitto
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
