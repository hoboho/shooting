package ir.hoboho.shooting.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private fun sample() = BackupPayload(
        exportedAtEpochMilli = 1_700_000_000_000L,
        settings = SettingsDto(themeMode = "DARK"),
        projects = listOf(ProjectDto("p-1", "پروژه \"الف\"", "منطقه ۳\nغرب", "کارفرما",
            "سرپرست", "توضیح", true, 1L, 2L)),
        workdays = listOf(WorkdayDto("w-1", "p-1", 20_759L, "منطقه", "کارفرما",
            "سرپرست", 3, "یادداشت", 800, 1L, 2L)),
        operations = listOf(OperationDto("o-1", "w-1", "ODC", "ODC-1", 900,
            "FAT", "FAT-2", 100, 24, 800, 1L, 2L)),
        cableTypes = listOf(CableTypeDto(24, "۲۴ کور", true, 2)),
        categories = listOf(CategoryDto("سوخت خودرو", true, 0)),
        expenses = listOf(ExpenseDto("e-1", 20_759L, "سوخت خودرو", 1_000_000L,
            "بنزین", "رسید ۱۲", "p-1", "w-1", 1L, 2L)),
        settlements = listOf(SettlementDto("s-1", "e-1", 400_000L, 20_760L, "واریز", 3L)),
    )

    @Test
    fun encodeDecode_roundTripsAllSections() {
        val text = BackupCodec.encode(sample())
        val result = BackupCodec.decode(text)
        assertTrue(result is BackupReadResult.Success)
        val payload = (result as BackupReadResult.Success).payload
        assertEquals(1, payload.formatVersion)
        assertEquals("DARK", payload.settings.themeMode)
        assertEquals(1, payload.projects.size)
        assertEquals("پروژه \"الف\"", payload.projects.single().name)
        assertEquals("منطقه ۳\nغرب", payload.projects.single().workArea)
        assertEquals(1, payload.operations.size)
        assertEquals(800, payload.operations.single().lengthMeters)
        assertEquals(1, payload.expenses.size)
        assertEquals(400_000L, payload.settlements.single().amountToman)
    }

    @Test
    fun tamperedPayload_isRejectedByChecksum() {
        val text = BackupCodec.encode(sample()).replace("پروژه \\\"الف\\\"", "پروژه تغییرکرده")
        assertTrue(BackupCodec.decode(text) is BackupReadResult.ChecksumMismatch)
    }

    @Test
    fun wrongMagicAndMalformedFiles_areRejected() {
        assertTrue(BackupCodec.decode("{\"magic\":\"OTHER\"}") is BackupReadResult.WrongMagic)
        assertTrue(BackupCodec.decode("this is not json") is BackupReadResult.Malformed)
        assertTrue(BackupCodec.decode("{}") is BackupReadResult.WrongMagic)
    }

    @Test
    fun newerFormatVersion_isRejected() {
        val text = BackupCodec.encode(sample().copy(formatVersion = 99))
        assertTrue(BackupCodec.decode(text) is BackupReadResult.UnsupportedVersion)
    }

    @Test
    fun emptyBackup_isValid() {
        val text = BackupCodec.encode(BackupPayload(exportedAtEpochMilli = 0L, settings = SettingsDto()))
        val result = BackupCodec.decode(text)
        assertTrue(result is BackupReadResult.Success)
        assertEquals(0, BackupCounts.from((result as BackupReadResult.Success).payload).total)
    }
}
