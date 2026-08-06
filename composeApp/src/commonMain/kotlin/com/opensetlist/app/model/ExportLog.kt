package com.opensetlist.app.model

/**
 * Tipo de uma linha do log de exportação em lote.
 *
 * @author ruanitto
 */
enum class ExportLogKind {
    START,
    DONE,
    FAILED,
    SUCCESS,
    INFO
}

/**
 * Uma linha do log de exportação em lote de músicas (.pro).
 *
 * @author ruanitto
 */
data class ExportLogEntry(
    val text: String,
    val kind: ExportLogKind = ExportLogKind.INFO
)
