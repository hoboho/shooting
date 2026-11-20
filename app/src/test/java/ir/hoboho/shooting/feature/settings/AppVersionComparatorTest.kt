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
    @Test
    fun latestReleaseIgnoresMalformedTagsAndUsesNumericOrder() {
        assertEquals("v2.10.1-release", latestReleaseTag(listOf(
            "v2.9.99-release", "v2.10.1-release", "v2.11-release", "v3.0.0-beta", "broken-release",
        )))
        assertEquals(null, latestReleaseTag(listOf("v3.0-release", "v1.0.0-beta")))
    }

}
