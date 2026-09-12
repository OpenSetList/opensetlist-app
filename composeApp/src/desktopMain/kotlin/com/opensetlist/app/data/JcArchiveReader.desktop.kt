package com.opensetlist.app.data

import java.util.zip.ZipInputStream

/**
 * Leitura do `data.json` de um arquivo ZIP no desktop, via java.util.zip.
 *
 * @author ruanitto
 */
actual fun readZipDataJson(bytes: ByteArray): String? = runCatching {
    ZipInputStream(bytes.inputStream()).use { stream ->
        while (true) {
            val entry = stream.nextEntry ?: break
            if (entry.name.equals(ZipData.ENTRY_NAME, ignoreCase = true)) {
                return stream.readBytes().decodeToString()
            }
        }
    }
    null
}.getOrNull()
