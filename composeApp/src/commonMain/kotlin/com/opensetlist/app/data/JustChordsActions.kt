package com.opensetlist.app.data

import androidx.compose.runtime.Composable

/**
 * Ações de importação de setlist no formato JustChords (.chopro/.jcarchive) por
 * plataforma. Entrega o nome do arquivo (usado como nome do setlist) e os bytes,
 * para que a extensão decida entre .chopro (texto) e .jcarchive (ZIP).
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
    onImported: (fileName: String, bytes: ByteArray) -> Unit
): JustChordsActions
