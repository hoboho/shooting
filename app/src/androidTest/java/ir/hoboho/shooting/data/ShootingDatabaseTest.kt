package ir.hoboho.shooting.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import ir.hoboho.shooting.core.common.Clock
import ir.hoboho.shooting.core.common.ValidationException
import ir.hoboho.shooting.data.local.ShootingDatabase
import ir.hoboho.shooting.data.mapper.toDomain
import ir.hoboho.shooting.data.repository.BackupRepositoryImpl
import ir.hoboho.shooting.data.repository.ExpenseRepositoryImpl
import ir.hoboho.shooting.data.repository.ProjectRepositoryImpl
import ir.hoboho.shooting.data.repository.WorkdayRepositoryImpl
import ir.hoboho.shooting.di.SeedCallback
import ir.hoboho.shooting.domain.backup.RestoreStrategy
import ir.hoboho.shooting.domain.model.Expense
import ir.hoboho.shooting.domain.model.PointType
import ir.hoboho.shooting.domain.model.Project
import ir.hoboho.shooting.domain.model.Settlement
import ir.hoboho.shooting.domain.model.ShootingOperation
import ir.hoboho.shooting.domain.model.Workday
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device Room/repository tests. Run with `./gradlew connectedDebugAndroidTest`.
 * These verify the parts that cannot be tested on a plain JVM: SQLite foreign
 * keys, the unique (project, date) index, transaction-safe totals and restore.
 */
@RunWith(AndroidJUnit4::class)
class ShootingDatabaseTest {

    private lateinit var db: ShootingDatabase
    private val clock = object : Clock { override fun nowEpochMilli() = 1_700_000_000_000L }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
        db, db.expenseDao(), db.settlementDao(), db.expenseCategoryDao(), clock,
    )
    private fun backups() = BackupRepositoryImpl(
        db, db.projectDao(), db.workdayDao(), db.operationDao(), db.cableTypeDao(),
        db.expenseCategoryDao(), db.expenseDao(), db.settlementDao(), db.appSettingDao(),
    )

    @Test
    fun seed_installsExtensibleReferenceData() = runBlocking {
        assertThat(db.cableTypeDao().getAll().map { it.coreCount })
            .containsExactly(2, 4, 6, 12, 24, 36, 48, 72, 96, 144, 288).inOrder()
        assertThat(db.expenseCategoryDao().getAll()).hasSize(7)
        // Adding a new core count is an ordinary insert, not a schema change.
        assertThat(
            ir.hoboho.shooting.data.repository.ReferenceRepositoryImpl(
                db.cableTypeDao(), db.expenseCategoryDao(),
            ).addCableType(8),
        ).isGreaterThan(0L)
        assertThat(db.cableTypeDao().getByCoreCount(8)).isNotNull()
    }

    @Test
    fun workdaySave_recomputesAbsoluteLengthsAndTotal() = runBlocking {
        val projectId = projects().save(Project(name = "پروژه الف", workArea = "منطقه ۱"))
        val cableId = db.cableTypeDao().getByCoreCount(24)!!.id
        val day = 20_000L
        val id = workdays().save(
            Workday(projectId = projectId, dateEpochDay = day, workerCount = 3),
            listOf(
                ShootingOperation(
                    startType = PointType.ODC, startCode = "A1", startMeter = 980,
                    endType = PointType.FAT, endCode = "B2", endMeter = 120,
                    cableTypeId = cableId,
                ),
                ShootingOperation(
                    startType = PointType.HANDHOLE, startCode = "H1", startMeter = 0,
                    endType = PointType.MANHOLE, endCode = "M1", endMeter = 500,
                    cableTypeId = cableId,
                ),
            ),
        )
        val detail = db.workdayDao().getDetail(id)!!.toDomain()
        assertThat(detail.operations.map { it.lengthMeters }).containsExactly(860, 500).inOrder()
        assertThat(detail.totalLengthMeters).isEqualTo(1360)
    }

    @Test
    fun duplicateProjectDate_isRejectedBeforeWriting() = runBlocking {
        val projectId = projects().save(Project(name = "پروژه ب"))
        val cableId = db.cableTypeDao().getByCoreCount(12)!!.id
        fun operation() = ShootingOperation(
            startCode = "A", startMeter = 0, endCode = "B", endMeter = 10,
            cableTypeId = cableId,
        )
        workdays().save(Workday(projectId = projectId, dateEpochDay = 20_001L), listOf(operation()))
        var rejected = false
        try {
            workdays().save(Workday(projectId = projectId, dateEpochDay = 20_001L), listOf(operation()))
        } catch (e: ValidationException) {
            rejected = true
        }
        assertThat(rejected).isTrue()
        assertThat(db.workdayDao().getAll()).hasSize(1)
    }

    @Test
    fun settlements_neverExceedOutstanding_andDeletionIsGuarded() = runBlocking {
        val categoryId = db.expenseCategoryDao().getAll().first().id
        val expenseId = expenses().save(
            Expense(dateEpochDay = 20_000L, categoryId = categoryId, amountToman = 1_000L, companyApproved = true),
        )
        expenses().addSettlement(Settlement(expenseId = expenseId, amountToman = 400L, dateEpochDay = 20_001L))
        assertThat(expenses().getExpense(expenseId)!!.settledAmountToman).isEqualTo(400L)
        assertThat(expenses().getExpense(expenseId)!!.outstandingToman).isEqualTo(600L)

        var rejected = false
        try {
            expenses().addSettlement(
                Settlement(expenseId = expenseId, amountToman = 601L, dateEpochDay = 20_002L),
            )
        } catch (e: ValidationException) {
            rejected = true
        }
        assertThat(rejected).isTrue()
        assertThat(expenses().getExpense(expenseId)!!.settledAmountToman).isEqualTo(400L)

        var deleteRejected = false
        try {
            expenses().delete(expenseId)
        } catch (e: ValidationException) {
            deleteRejected = true
        }
        assertThat(deleteRejected).isTrue()
        assertThat(expenses().getExpense(expenseId)).isNotNull()
    }

    @Test
    fun companyPayment_allocatesOldestApprovedFirst_andAllowsManualAdjustment() = runBlocking {
        val categoryId = db.expenseCategoryDao().getAll().first().id
        val oldestId = expenses().save(
            Expense(dateEpochDay = 20_000L, categoryId = categoryId, amountToman = 1_000L, companyApproved = true),
        )
        val nextId = expenses().save(
            Expense(dateEpochDay = 20_001L, categoryId = categoryId, amountToman = 1_500L, companyApproved = true),
        )
        val unapprovedId = expenses().save(
            Expense(dateEpochDay = 20_002L, categoryId = categoryId, amountToman = 2_000L, companyApproved = false),
        )

        expenses().allocateCompanyPayment(1_250L, 20_003L, "واریز شرکت")
        assertThat(expenses().getExpense(oldestId)!!.settledAmountToman).isEqualTo(1_000L)
        assertThat(expenses().getExpense(nextId)!!.settledAmountToman).isEqualTo(250L)
        assertThat(expenses().getExpense(unapprovedId)!!.settledAmountToman).isEqualTo(0L)

        val nextAllocation = expenses().getAllSettlements().single { it.expenseId == nextId }
        expenses().updateSettlement(nextAllocation.copy(amountToman = 150L))
        assertThat(expenses().getExpense(nextId)!!.settledAmountToman).isEqualTo(150L)
        assertThat(expenses().getExpense(nextId)!!.outstandingToman).isEqualTo(1_350L)
    }

    @Test
    fun addOnlyRestore_doesNotDuplicateOrLoseRows() = runBlocking {
        val projectId = projects().save(Project(name = "پروژه ج"))
        val cableId = db.cableTypeDao().getByCoreCount(48)!!.id
        workdays().save(
            Workday(projectId = projectId, dateEpochDay = 20_000L),
            listOf(ShootingOperation(
                startCode = "A", startMeter = 0, endCode = "B", endMeter = 75,
                cableTypeId = cableId,
            )),
        )
        val before = backups().snapshot()
        assertThat(before.projects).hasSize(1)
        assertThat(before.operations).hasSize(1)

        val imported = backups().restore(before, RestoreStrategy.ADD_ONLY)
        assertThat(imported.projects).isEqualTo(1)
        assertThat(db.projectDao().getAll()).hasSize(1)
        assertThat(db.workdayDao().getAll()).hasSize(1)
        assertThat(db.workdayDao().getAll().single().totalLengthMeters).isEqualTo(75)
        assertThat(db.operationDao().getAll()).hasSize(1)
    }
}
