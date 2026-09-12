package com.opensetlist.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OslArchiveTest {

    private val sampleJson = """{"type":"setlist_app_set","version":1,"setlist":{},"songs":[]}"""

    @Test
    fun build_producesZipArchive() {
        val zip = OslArchive.build(sampleJson)
        assertTrue(zip.size > 4)
        assertEquals('P'.code.toByte(), zip[0])
        assertEquals('K'.code.toByte(), zip[1])
        assertEquals(0x03, zip[2].toInt() and 0xFF)
        assertEquals(0x04, zip[3].toInt() and 0xFF)
    }

    @Test
    fun isZipArchive_detectsBuiltZip() {
        assertTrue(ZipData.isZipArchive(OslArchive.build(sampleJson)))
    }

    @Test
    fun isZipArchive_rejectsPlainTextAndEmpty() {
        assertFalse(ZipData.isZipArchive(sampleJson.encodeToByteArray()))
        assertFalse(ZipData.isZipArchive(ByteArray(0)))
    }
}