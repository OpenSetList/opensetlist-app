package com.opensetlist.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonParserTest {

    @Test
    fun parseObject_flat() {
        val obj = JsonParser("""{"a":1,"b":"x"}""").parseObject()
        assertEquals(1L, obj?.get("a"))
        assertEquals("x", obj?.get("b"))
    }

    @Test
    fun parseObject_nested() {
        val obj = JsonParser("""{"out":{"in":[1,2,3]}}""").parseObject()
        val inner = obj?.get("out") as Map<*, *>
        val arr = inner["in"] as List<*>
        assertEquals(listOf(1L, 2L, 3L), arr)
    }

    @Test
    fun parseObject_booleansAndNull() {
        val obj = JsonParser("""{"t":true,"f":false,"n":null}""").parseObject()
        assertEquals(true, obj?.get("t"))
        assertEquals(false, obj?.get("f"))
        assertNull(obj?.get("n"))
    }

    @Test
    fun parseObject_escapedString() {
        val obj = JsonParser("""{"k":"a\"b\\c\nd"}""").parseObject()
        assertEquals("a\"b\\c\nd", obj?.get("k"))
    }

    @Test
    fun parseObject_decimalNumber_returnsDouble() {
        val obj = JsonParser("""{"pi":3.14}""").parseObject()
        assertEquals(3.14, obj?.get("pi"))
    }

    @Test
    fun parseObject_empty() {
        assertEquals(emptyMap<String, Any?>(), JsonParser("{}").parseObject())
    }

    @Test
    fun parseObject_invalid_returnsNull() {
        assertNull(JsonParser("").parseObject())
        assertNull(JsonParser("[1,2]").parseObject())
        assertNull(JsonParser("""{"a":}""").parseObject())
    }
}
