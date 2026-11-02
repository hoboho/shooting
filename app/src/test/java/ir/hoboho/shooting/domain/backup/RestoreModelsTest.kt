package ir.hoboho.shooting.domain.backup

import ir.hoboho.shooting.core.backup.BackupCounts
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreModelsTest {
    @Test
    fun strategyFlagsAreExplicit() {
        val preview = RestorePreview(
            formatVersion = 1, appVersionName = "1.0.0", exportedAtEpochMilli = 1L,
            incoming = BackupCounts(projects = 3), existing = BackupCounts(projects = 2),
            strategy = RestoreStrategy.REPLACE, formatSupported = true,
        )
        assertTrue(preview.willReplaceExistingData)
        assertFalse(preview.willOverwriteExistingData)
        assertTrue(preview.copy(strategy = RestoreStrategy.MERGE).willOverwriteExistingData)
        assertTrue(preview.copy(strategy = RestoreStrategy.ADD_ONLY).let {
            !it.willReplaceExistingData && !it.willOverwriteExistingData
        })
    }
}
