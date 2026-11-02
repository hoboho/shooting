package org.junit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Test

object Assert {
    fun assertEquals(expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("expected <$expected> but was <$actual>")
    }
    fun assertEquals(message: String, expected: Any?, actual: Any?) {
        if (expected != actual) throw AssertionError("$message: expected <$expected> but was <$actual>")
    }
    fun assertTrue(condition: Boolean) { if (!condition) throw AssertionError("expected true") }
    fun assertTrue(message: String, condition: Boolean) { if (!condition) throw AssertionError(message) }
    fun assertFalse(condition: Boolean) { if (condition) throw AssertionError("expected false") }
    fun assertNull(actual: Any?) { if (actual != null) throw AssertionError("expected null but was <$actual>") }
    fun assertNotNull(actual: Any?) { if (actual == null) throw AssertionError("expected non-null") }
    fun assertNotEquals(unexpected: Any?, actual: Any?) {
        if (unexpected == actual) throw AssertionError("expected value different from <$unexpected>")
    }
    fun fail(message: String): Nothing = throw AssertionError(message)
}
