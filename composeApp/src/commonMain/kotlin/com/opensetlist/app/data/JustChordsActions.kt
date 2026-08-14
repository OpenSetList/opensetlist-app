package com.opensetlist.app.data

import androidx.compose.runtime.Composable

/**
 * Ações de importação de setlist no formato JustChords (.chopro) por plataforma.
 * Entrega o nome do arquivo (usado como nome do setlist) e o conteúdo.
 *
 * @author ruanitto
 */
class JustChordsActions(
    val importFile: () -> Unit
)

/**
 * Cria as ações de importação JustChords conforme a plataforma atual.
 */
@Composable
expect fun rememberJustChordsActions(
    onImported: (fileName: String, content: String) -> Unit
): JustChordsActions
