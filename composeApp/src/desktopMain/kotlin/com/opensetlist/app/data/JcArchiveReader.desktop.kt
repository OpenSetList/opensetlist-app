package com.opensetlist.app.data

import java.util.zip.ZipInputStream

/**
 * Leitura do `data.json` de um arquivo .jcarchive no desktop, via java.util.zip.
 *
 * @author ruanitto
 */
actual fun readJcArchiveDataJson(bytes: ByteArray): String? = runCatching {
    ZipInputStream(bytes.inputStream()).use { stream ->
        while (true) {
            val entry = stream.nextEntry ?: break
            if (entry.name.equals(JcArchive.ENTRY_NAME, ignoreCase = true)) {
                return stream.readBytes().decodeToString()
            }
        }
    }
    null
}.getOrNull()
