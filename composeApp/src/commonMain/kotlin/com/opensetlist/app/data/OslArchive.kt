package com.opensetlist.app.data

object OslArchive {

    const val FILE_EXTENSION = "osl"

    fun build(dataJson: String): ByteArray = ZipData.buildDataJsonZip(dataJson)

    fun readDataJson(bytes: ByteArray): String? = readZipDataJson(bytes)
}