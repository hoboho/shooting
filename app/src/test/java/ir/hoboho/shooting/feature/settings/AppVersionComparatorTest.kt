package ir.hoboho.shooting.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionComparatorTest {

    @Test
    fun comparesEachNumericComponentInsteadOfLexicographicStrings() {
        assertEquals(1, compareAppVersions("2.2.0", "2.1.0"))
        assertEquals(1, compareAppVersions("2.10.0", "2.9.0"))
        assertEquals(1, compareAppVersions("2.1.10", "2.1.9"))
        assertEquals(-1, compareAppVersions("2.1.0", "2.2.0"))
        assertEquals(0, compareAppVersions("2.2.0", "v2.2.0"))
    }
}
