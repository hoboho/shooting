package ir.hoboho.shooting.workflow

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hoboho.shooting.core.backup.AppInfo
import ir.hoboho.shooting.core.backup.BackupCodec
import ir.hoboho.shooting.core.backup.BackupException
import ir.hoboho.shooting.core.backup.BackupManager
import ir.hoboho.shooting.core.backup.BackupMapper
import ir.hoboho.shooting.core.backup.BackupReadResult
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.core.date.DateRange
import ir.hoboho.shooting.core.pdf.PdfDocumentSpec
import ir.hoboho.shooting.core.pdf.PdfReportEngine
import ir.hoboho.shooting.data.local.ShootingDatabase
import ir.hoboho.shooting.data.repository.BackupRepositoryImpl
import ir.hoboho.shooting.data.repository.ExpenseRepositoryImpl
import ir.hoboho.shooting.data.repository.ProjectRepositoryImpl
import ir.hoboho.shooting.data.repository.ReferenceRepositoryImpl
import ir.hoboho.shooting.data.repository.SettingsRepositoryImpl
import ir.hoboho.shooting.data.repository.WorkdayRepositoryImpl
import ir.hoboho.shooting.di.SeedCallback
import ir.hoboho.shooting.domain.PeriodAggregator
import ir.hoboho.shooting.domain.backup.RestoreStrategy
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.SettlementStatus
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import ir.hoboho.shooting.domain.report.ExpenseReportRow
import ir.hoboho.shooting.domain.report.ReportSpecBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end on-device verification of the workflows the brief calls out:
 * daily reporting (including a reversed reel), partial/full settlements,
 * separation of length and money, all three PDF exports and a versioned,
 * previewed, safety-snapshot backup/restore round trip.
 *
 * These are automated integration checks, not a human visual inspection.
 */
@RunWith(AndroidJUnit4::class)
class WorkflowIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: ShootingDatabase

    /** Advances on every read so backup and safety-snapshot filenames differ. */
    private var now = 1_700_000_000_000L
    private val clock = object : Clock {
        override fun nowEpochMilli(): Long = now.also { now += 1_000L }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "backups").deleteRecursively()
        File(context.filesDir, "qa").deleteRecursively()
        db = Room.inMemoryDatabaseBuilder(context, ShootingDatabase::class.java)
            .addCallback(SeedCallback)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun projects() = ProjectRepositoryImpl(db.projectDao(), clock)

    private fun workdays() = WorkdayRepositoryImpl(
        db, db.workdayDao(), db.operationDao(), db.projectDao(), db.cableTypeDao(), clock,
    )

    private fun expenses() = ExpenseRepositoryImpl(
        db, db.expenseDao(), db.settlementDao(), db.expenseCategoryDao(), db.companyPaymentDao(), clock,
    )

    private fun references() = ReferenceRepositoryImpl(db.cableTypeDao(), db.expenseCategoryDao())

    private fun backups() = BackupRepositoryImpl(
        db, db.projectDao(), db.workdayDao(), db.operationDao(), db.cableTypeDao(),
        db.expenseCategoryDao(), db.expenseDao(), db.settlementDao(), db.companyPaymentDao(), db.appSettingDao(),
    )

    private fun settings() = SettingsRepositoryImpl(db.appSettingDao())

    private fun manager() = BackupManager(context, backups(), settings(), clock)

    /** App-private but reachable from CI via `run-as`; no storage permission needed. */
    private fun outputDir(): File = File(context.filesDir, "qa").apply { mkdirs() }

    private fun operation(cableId: Long, start: Int, end: Int) = ShootingOperation(
        startType = PointType.ODC,
        startCode = "A$start",
        startMeter = start,
        endType = PointType.FAT,
        endCode = "B$end",
        endMeter = end,
        cableTypeId = cableId,
    )

    private fun render(spec: PdfDocumentSpec, name: String): File {
        val file = File(outputDir(), name)
        if (file.exists()) file.delete()
        PdfReportEngine(context).render(spec, file)
        assertReadablePdf(file)
        publishToHost(file)
        return file
    }

    /**
     * Gradle uninstalls the APKs after connectedAndroidTest, so app-private
     * files disappear. MediaStore.Downloads is public storage that survives the
     * uninstall; CI pulls these samples from Download/ShootingQA.
     */
    private fun publishToHost(file: File) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/ShootingQA",
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching
            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }

    /** A PDF that opens in Android's renderer and actually contains dark text. */
    private fun assertReadablePdf(file: File) {
        assertThat(file.exists()).isTrue()
        assertThat(file.length()).isGreaterThan(1_000L)
        val header = file.inputStream().use { stream ->
            ByteArray(5).also { bytes -> stream.read(bytes) }
        }
        assertThat(String(header, Charsets.US_ASCII)).isEqualTo("%PDF-")

        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        try {
            assertThat(renderer.pageCount).isAtLeast(1)
            val page = renderer.openPage(0)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            var darkSamples = 0
            for (y in 0 until bitmap.height step 4) {
                for (x in 0 until bitmap.width step 4) {
                    val pixel = bitmap.getPixel(x, y)
                    if (Color.red(pixel) < 180 && Color.green(pixel) < 180 && Color.blue(pixel) < 180) {
                        darkSamples++
                    }
                }
            }
            assertThat(darkSamples).isGreaterThan(20)
        } finally {
            renderer.close()
            descriptor.close()
        }
    }

    @Test
    fun dailyWorkflow_reverseReelDuplicatePrevention_andDailyPdf() = runBlocking {
        val projectId = projects().save(
            Project(name = "پروژه الف", workArea = "منطقه ۱", employer = "کارفرما", supervisor = "سرپرست"),
        )
        val cableId = db.cableTypeDao().getByCoreCount(24)!!.id
        val day = 20_700L
        val workdayId = workdays().save(
            Workday(
                projectId = projectId,
                dateEpochDay = day,
                workArea = "منطقه ۱",
                employer = "کارفرما",
                supervisor = "سرپرست",
                workerCount = 4,
                notes = "قرقره معکوس",
            ),
            listOf(
                operation(cableId, start = 1_000, end = 200),
                operation(cableId, start = 0, end = 125),
            ),
        )

        val detail = workdays().getWorkday(workdayId)!!
        assertThat(detail.projectName).isEqualTo("پروژه الف")
        assertThat(detail.operations.map { it.lengthMeters }).containsExactly(800, 125).inOrder()
        assertThat(detail.totalLengthMeters).isEqualTo(925)

        var duplicateRejected = false
        try {
            workdays().save(
                Workday(projectId = projectId, dateEpochDay = day),
                listOf(operation(cableId, start = 0, end = 10)),
            )
        } catch (e: ValidationException) {
            duplicateRejected = true
        }
        assertThat(duplicateRejected).isTrue()
        assertThat(db.workdayDao().getAll()).hasSize(1)

        render(
            ReportSpecBuilder.daily(detail, references().getAllCableTypes()),
            "daily-workflow.pdf",
        )
        Unit
    }

    @Test
    fun expenseWorkflow_partialAndFullSettlement_andSeparatePeriodAndExpensePdfs() = runBlocking {
        val projectId = projects().save(Project(name = "پروژه ب"))
        val cableId = db.cableTypeDao().getByCoreCount(12)!!.id
        val day = 20_701L
        val workdayId = workdays().save(
            Workday(projectId = projectId, dateEpochDay = day),
            listOf(operation(cableId, start = 0, end = 300)),
        )
        val categoryId = db.expenseCategoryDao().getAll().first().id
        val expenseId = expenses().save(
            Expense(
                dateEpochDay = day,
                categoryId = categoryId,
                amountToman = 1_000L,
                description = "سوخت خودرو",
                projectId = projectId,
                workdayId = workdayId,
                companyApproved = true,
            ),
        )

        expenses().addSettlement(Settlement(expenseId = expenseId, amountToman = 300L, dateEpochDay = day))
        expenses().addSettlement(Settlement(expenseId = expenseId, amountToman = 200L, dateEpochDay = day + 1))
        val partial = expenses().getExpense(expenseId)!!
        assertThat(partial.settledAmountToman).isEqualTo(500L)
        assertThat(partial.outstandingToman).isEqualTo(500L)
        assertThat(partial.status).isEqualTo(SettlementStatus.PARTIAL)

        expenses().addSettlement(Settlement(expenseId = expenseId, amountToman = 500L, dateEpochDay = day + 2))
        val settled = expenses().getExpense(expenseId)!!
        assertThat(settled.settledAmountToman).isEqualTo(1_000L)
        assertThat(settled.outstandingToman).isEqualTo(0L)
        assertThat(settled.status).isEqualTo(SettlementStatus.SETTLED)

        var excessRejected = false
        try {
            expenses().addSettlement(
                Settlement(expenseId = expenseId, amountToman = 1L, dateEpochDay = day + 3),
            )
        } catch (e: ValidationException) {
            excessRejected = true
        }
        assertThat(excessRejected).isTrue()

        val range = DateRange(day - 1, day + 3)
        val allWorkdays = workdays().getAllWorkdaysWithOperations()
        val allExpenses = expenses().getAllExpenses()
        val report = PeriodAggregator.build(
            range = range,
            workdays = allWorkdays,
            expenses = allExpenses,
            cableTypes = references().getAllCableTypes(),
        )
        assertThat(report.totalLengthMeters).isEqualTo(300)
        assertThat(report.totalExpensesToman).isEqualTo(1_000L)
        assertThat(report.settledToman).isEqualTo(1_000L)
        assertThat(report.outstandingToman).isEqualTo(0L)
        render(ReportSpecBuilder.period(report), "period-workflow.pdf")

        val settlements = expenses().getAllSettlements()
        val rows = allExpenses.map { expense ->
            ExpenseReportRow(expense, settlements.filter { it.expenseId == expense.id })
        }
        render(ReportSpecBuilder.expenses(rows, range), "expenses-workflow.pdf")
        Unit
    }

    @Test
    fun backupWorkflow_versionedPreviewSafetySnapshotAndIntegrity() = runBlocking {
        val projectId = projects().save(Project(name = "پروژه پایه"))
        val cableId = db.cableTypeDao().getByCoreCount(48)!!.id
        val day = 20_702L
        val workdayId = workdays().save(
            Workday(projectId = projectId, dateEpochDay = day),
            listOf(operation(cableId, start = 0, end = 80)),
        )
        val categoryId = db.expenseCategoryDao().getAll().first().id
        val expenseId = expenses().save(
            Expense(
                dateEpochDay = day,
                categoryId = categoryId,
                amountToman = 500L,
                projectId = projectId,
                workdayId = workdayId,
                companyApproved = true,
            ),
        )
        expenses().addSettlement(
            Settlement(expenseId = expenseId, amountToman = 200L, dateEpochDay = day),
        )

        val manager = manager()
        val file = manager.createBackup()
        assertThat(file.name).endsWith(AppInfo.BACKUP_FILE_EXTENSION)
        val text = file.readText()
        val decoded = BackupCodec.decode(text)
        assertThat(decoded).isInstanceOf(BackupReadResult.Success::class.java)
        val payload = (decoded as BackupReadResult.Success).payload
        assertThat(payload.formatVersion).isEqualTo(BackupCodec.FORMAT_VERSION)
        assertThat(BackupMapper.validate(payload)).isEmpty()

        val inspection = manager.inspect(text, RestoreStrategy.REPLACE)
        assertThat(inspection.preview.formatSupported).isTrue()
        assertThat(inspection.preview.willReplaceExistingData).isTrue()
        assertThat(inspection.warnings).isEmpty()
        assertThat(inspection.preview.incoming.projects).isEqualTo(1)
        assertThat(inspection.preview.incoming.workdays).isEqualTo(1)
        assertThat(inspection.preview.incoming.operations).isEqualTo(1)
        assertThat(inspection.preview.incoming.expenses).isEqualTo(1)
        assertThat(inspection.preview.incoming.settlements).isEqualTo(1)

        // Preview must not write a single row.
        val beforePreview = backups().counts()
        assertThat(backups().counts()).isEqualTo(beforePreview)

        val extraProjectId = projects().save(Project(name = "پروژه اضافی"))
        assertThat(backups().counts().projects).isEqualTo(2)

        val outcome = manager.restore(text, RestoreStrategy.REPLACE)
        assertThat(outcome.strategy).isEqualTo(RestoreStrategy.REPLACE)
        assertThat(outcome.safetySnapshotFile).isNotNull()
        val safety = File(File(context.filesDir, "backups"), outcome.safetySnapshotFile!!)
        assertThat(safety.exists()).isTrue()
        assertThat(BackupCodec.decode(safety.readText()))
            .isInstanceOf(BackupReadResult.Success::class.java)
        assertThat(backups().counts().projects).isEqualTo(1)
        assertThat(db.projectDao().getById(extraProjectId)).isNull()
        assertThat(db.workdayDao().getAll().single().totalLengthMeters).isEqualTo(80)
        assertThat(db.settlementDao().getAll()).hasSize(1)

        // A tampered checksum is rejected before any data is written.
        val match = Regex("\"sha256\":\"([0-9a-f]{64})\"").find(text)!!
        val original = match.groupValues[1]
        val changed = (if (original.first() == '0') '1' else '0') + original.drop(1)
        val tampered = text.replaceRange(match.range, "\"sha256\":\"$changed\"")
        var tamperRejected = false
        try {
            manager.inspect(tampered, RestoreStrategy.ADD_ONLY)
        } catch (e: BackupException.InvalidFile) {
            tamperRejected = true
        }
        assertThat(tamperRejected).isTrue()
        assertThat(backups().counts().projects).isEqualTo(1)
        Unit
    }
}
