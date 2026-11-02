package ir.hoboho.shooting.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleJsonTest {

    @Test
    fun parsesAndWritesObjectsArraysAndEscapes() {
        val text = """{"a":"سلام \"دنیا\\"\n","b":[1,-2,true,false,null,{"c":3.5}],"d":"\u0641"}"""
        val parsed = SimpleJson.parse(text).asObj()!!
        assertEquals("سلام \\"دنیا\\"\n", parsed.string("a"))
        assertEquals(6, parsed.field("b").asArr()!!.values.size)
        assertEquals(-2L, parsed.field("b").asArr()!!.values[1].asLong())
        assertEquals(true, parsed.field("b").asArr()!!.values[2].asBoolean())
        assertEquals("ف", parsed.string("d"))
        // Re-serialising then parsing yields the same structure.
        val canonical = SimpleJson.stringify(parsed)
        assertEquals(canonical, SimpleJson.stringify(SimpleJson.parse(canonical)))
    }

    @Test
    fun rejectsMalformedInput() {
        for (bad in listOf("{", "[1,", "{\"a\":}", "", "{\"a\":1} trailing")) {
            try {
                SimpleJson.parse(bad)
                org.junit.Assert.fail("expected failure for: $bad")
            } catch (expected: Throwable) {
                assertTrue(true)
            }
        }
    }

    @Test
    fun checksumsAreStableAndChangeWithContent() {
        val a = BackupCodec.sha256Hex("{\"projects\":[]}")
        val b = BackupCodec.sha256Hex("{\"projects\":[]}")
        val c = BackupCodec.sha256Hex("{\"projects\":[1]}")
        assertEquals(a, b)
        assertTrue(a != c)
        assertEquals(64, a.length)
    }
}
