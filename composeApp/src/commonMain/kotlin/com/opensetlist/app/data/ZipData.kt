package com.opensetlist.app.data

object ZipData {

    internal const val ENTRY_NAME = "data.json"

    fun isZipArchive(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()

    fun buildDataJsonZip(dataJson: String): ByteArray {
        val name = ENTRY_NAME.encodeToByteArray()
        val content = dataJson.encodeToByteArray()
        val crc = crc32(content)
        val size = content.size
        val out = ByteArraySink()
        out.writeInt(0x04034b50)
        out.writeShort(20)
        out.writeShort(0x0800)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeInt(crc)
        out.writeInt(size)
        out.writeInt(size)
        out.writeShort(name.size)
        out.writeShort(0)
        out.write(name)
        out.write(content)
        val centralStart = out.size
        out.writeInt(0x02014b50)
        out.writeShort(20)
        out.writeShort(20)
        out.writeShort(0x0800)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeInt(crc)
        out.writeInt(size)
        out.writeInt(size)
        out.writeShort(name.size)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(0)
        out.writeInt(0)
        out.writeInt(0)
        out.write(name)
        val centralEnd = out.size
        out.writeInt(0x06054b50)
        out.writeShort(0)
        out.writeShort(0)
        out.writeShort(1)
        out.writeShort(1)
        out.writeInt(centralEnd - centralStart)
        out.writeInt(centralStart)
        out.writeShort(0)
        return out.toByteArray()
    }

    private fun crc32(bytes: ByteArray): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (byte in bytes) {
            crc = crc xor (byte.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
            }
        }
        return crc.inv()
    }

    private class ByteArraySink(initialCapacity: Int = 256) {
        private var bytes = ByteArray(initialCapacity)
        var size = 0
            private set

        val capacity: Int get() = bytes.size

        fun write(value: Int) {
            ensure(1)
            bytes[size++] = (value and 0xFF).toByte()
        }

        fun writeShort(value: Int) {
            write(value)
            write(value ushr 8)
        }

        fun writeInt(value: Int) {
            write(value)
            write(value ushr 8)
            write(value ushr 16)
            write(value ushr 24)
        }

        fun write(src: ByteArray) {
            ensure(src.size)
            src.copyInto(bytes, destinationOffset = size)
            size += src.size
        }

        fun toByteArray(): ByteArray = bytes.copyOf(size)

        private fun ensure(extra: Int) {
            if (size + extra > bytes.size) {
                bytes = bytes.copyOf(maxOf(bytes.size * 2, size + extra))
            }
        }
    }
}

expect fun readZipDataJson(bytes: ByteArray): String?