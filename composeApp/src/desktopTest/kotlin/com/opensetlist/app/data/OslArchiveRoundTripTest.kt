package com.opensetlist.app.data

import java.io.File
import java.util.zip.ZipFile
import kotlin.test.Test
import kotlin.test.assertEquals

class OslArchiveRoundTripTest {

    @Test
    fun build_readDataJson_roundTrip() {
        val json = """{"type":"setlist_app_set","version":1,"setlist":{"id":1},"songs":[]}"""
        val zip = OslArchive.build(json)
        assertEquals(json, OslArchive.readDataJson(zip))
    }

    @Test
    fun build_producesZipReadableByJdk() {
        val json = """{"type":"setlist_app_set","version":1,"setlist":{"id":1},"songs":[]}"""
        val zip = OslArchive.build(json)
        val tmp = File.createTempFile("osl_test", ".osl")
        try {
            tmp.writeBytes(zip)
            ZipFile(tmp).use { zf ->
                assertEquals(1, zf.size())
                val entry = zf.entries().asSequence().single()
                assertEquals(ZipData.ENTRY_NAME, entry.name)
                assertEquals(json, zf.getInputStream(entry).readBytes().decodeToString())
            }
        } finally {
            tmp.delete()
        }
    }
}