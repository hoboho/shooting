package ir.hoboho.shooting.feature.reports

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportSelectionTest {
    @Test
    fun selectionIsKeptOnlyWhileVisible() {
        assertEquals(2L, selectVisibleWorkday(2L, listOf(1L, 2L)))
        assertEquals(3L, selectVisibleWorkday(2L, listOf(3L, 4L)))
        assertEquals(null, selectVisibleWorkday(2L, emptyList()))
        assertEquals(1L, selectVisibleWorkday(null, listOf(1L)))
    }
}
