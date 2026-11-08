package ir.hoboho.shooting.core.backup

import ir.hoboho.shooting.domain.model.SettlementStatus
import ir.hoboho.shooting.domain.model.AppSettings
import ir.hoboho.shooting.domain.model.CableType
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.ExpenseCategory
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.ThemeMode
import ir.hoboho.shooting.domain.model.Workday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupMapperTest {

    private fun sampleData() = BackupData(
        settings = AppSettings(themeMode = ThemeMode.DARK, defaultProjectId = 1L, lastBackupEpochMilli = 42L),
        projects = listOf(Project(1L, "p1", "پروژه الف", "منطقه", "کارفرما", "سرپرست", "شرح", true, 10L, 20L)),
        workdays = listOf(Workday(1L, "w1", 1L, "پروژه الف", 20_759L, "منطقه", "کارفرما",
            "سرپرست", 3, "یادداشت", emptyList(), 800, 10L, 20L)),
        operations = listOf(ShootingOperation(1L, "o1", 1L,
            startMeter = 900, endMeter = 100, cableTypeId = 1L,
            startCode = "ODC-1", endCode = "FAT-2", lengthMeters = 800, createdAt = 10L, updatedAt = 20L)),
        cableTypes = listOf(CableType(1L, 24, "۲۴ کور", true, 2)),
        categories = listOf(ExpenseCategory(1L, "سوخت خودرو", true, 0)),
        expenses = listOf(Expense(1L, "e1", 20_759L, 1L, "سوخت خودرو", 1_000_000L,
            "بنزین", "رسید ۱۲", 1L, "پروژه الف", 1L, 400_000L, 2, 10L, 20L, companyApproved = true)),
        settlements = listOf(
            Settlement(1L, "s1", 1L, 250_000L, 20_760L, "واریز اول", 11L),
            Settlement(2L, "s2", 1L, 150_000L, 20_761L, "واریز دوم", 12L),
        ),
    )

    @Test
    fun domainToPayloadAndBack_preservesEverything() {
        val payload = BackupMapper.toPayload(sampleData(), "1.0.0", 1, 1_700_000_000_000L)
        assertEquals("p1", payload.projects.single().uuid)
        assertEquals("p1", payload.workdays.single().projectUuid)
        assertEquals("w1", payload.operations.single().workdayUuid)
        assertEquals("e1", payload.settlements.first().expenseUuid)
        assertEquals(24, payload.operations.single().cableTypeCoreCount)
        assertEquals("DARK", payload.settings.themeMode)

        val restored = BackupMapper.toData(payload)
        assertEquals(1, restored.projects.size)
        assertEquals("پروژه الف", restored.workdays.single().projectName)
        assertEquals(1L, restored.operations.single().workdayId)
        assertEquals(1L, restored.operations.single().cableTypeId)
        assertEquals(800, restored.operations.single().lengthMeters)
        assertEquals(1_000_000L, restored.expenses.single().amountToman)
        assertEquals(400_000L, restored.expenses.single().settledAmountToman)
        assertEquals(2, restored.expenses.single().settlementCount)
        assertEquals(SettlementStatus.PARTIAL, restored.expenses.single().status)
        assertEquals(600_000L, restored.expenses.single().outstandingToman)
        assertEquals(ThemeMode.DARK, restored.settings.themeMode)
        assertEquals(1L, restored.settings.defaultProjectId)
        assertEquals(42L, restored.settings.lastBackupEpochMilli)
    }

    @Test
    fun codecAndMapper_roundTripTogether() {
        val payload = BackupMapper.toPayload(sampleData(), "1.0.0", 1, 1L)
        val decoded = BackupCodec.decode(BackupCodec.encode(payload)) as BackupReadResult.Success
        assertEquals(payload, decoded.payload)
        assertEquals(8, BackupCounts.from(decoded.payload).total)
    }

    @Test
    fun lengthIsRecomputedOnImport_notTrustedFromFile() {
        val payload = BackupMapper.toPayload(sampleData(), "1.0.0", 1, 1L)
            .let { p -> p.copy(operations = p.operations.map { it.copy(lengthMeters = 999) }) }
        val restored = BackupMapper.toData(payload)
        assertEquals(800, restored.operations.single().lengthMeters)
    }

    @Test
    fun validationRejectsBrokenReferencesAndAmounts() {
        val good = BackupMapper.toPayload(sampleData(), "1.0.0", 1, 1L)
        assertTrue(BackupMapper.validate(good).isEmpty())

        val orphan = good.copy(operations = good.operations.map { it.copy(workdayUuid = "missing") })
        assertTrue(BackupMapper.validate(orphan).any { it.contains("عملیات بدون گزارش") })

        val overSettled = good.copy(
            settlements = good.settlements + SettlementDto("s3", "e1", 900_000L, 20_762L, "", 13L),
        )
        assertTrue(BackupMapper.validate(overSettled).any { it.contains("تسویه بیشتر") })

        val badLength = good.copy(operations = good.operations.map { it.copy(lengthMeters = 1) })
        assertTrue(BackupMapper.validate(badLength).any { it.contains("ناسازگار") })

        assertTrue(BackupMapper.validate(good.copy(formatVersion = 2)).any { it.contains("نسخه قالب") })
    }

    @Test
    fun importRejectsUnresolvableReferenceInsteadOfDroppingRows() {
        val payload = BackupMapper.toPayload(sampleData(), "1.0.0", 1, 1L)
            .let { p -> p.copy(expenses = p.expenses.map { it.copy(categoryName = "ناشناخته") }) }
        try {
            BackupMapper.toData(payload)
            fail("expected BackupIntegrityException")
        } catch (expected: BackupIntegrityException) {
            assertTrue(expected.message!!.contains("Unknown expense category"))
        }
    }
}
