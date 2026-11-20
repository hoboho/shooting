package ir.hoboho.shooting.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.hoboho.shooting.R
import ir.hoboho.shooting.feature.dashboard.DashboardScreen
import ir.hoboho.shooting.feature.expenses.ExpenseEditorScreen
import ir.hoboho.shooting.feature.expenses.ExpensesScreen
import ir.hoboho.shooting.feature.projects.ProjectEditorScreen
import ir.hoboho.shooting.feature.projects.ProjectsScreen
import ir.hoboho.shooting.feature.reports.ReportsScreen
import ir.hoboho.shooting.feature.settings.BackupScreen
import ir.hoboho.shooting.feature.settings.ReferencesScreen
import ir.hoboho.shooting.feature.settings.SettingsScreen
import ir.hoboho.shooting.feature.workdays.WorkdayDetailScreen
import ir.hoboho.shooting.feature.workdays.WorkdayEditorScreen
import ir.hoboho.shooting.feature.workdays.WorkdaysScreen

/** String routes keep navigation dependency-free (no serialization plugin). */
object Routes {
    const val DASHBOARD = "dashboard"
    const val PROJECTS = "projects"
    const val WORKDAYS = "workdays"
    const val EXPENSES = "expenses"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val REFERENCES = "references"

    const val PROJECT_EDITOR = "project_editor/{projectId}"
    const val WORKDAY_EDITOR = "workday_editor/{workdayId}/{projectId}/{forceNew}"
    const val WORKDAY_DETAIL = "workday_detail/{workdayId}"
    const val EXPENSE_EDITOR = "expense_editor/{expenseId}"

    fun projectEditor(id: Long) = "project_editor/$id"
    fun workdayEditor(workdayId: Long, projectId: Long = 0L, forceNew: Boolean = true) = "workday_editor/$workdayId/$projectId/$forceNew"
    fun workdayDetail(id: Long) = "workday_detail/$id"
    fun expenseEditor(id: Long) = "expense_editor/$id"
}

enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Dashboard(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.SpaceDashboard, Icons.Filled.SpaceDashboard),
    Projects(Routes.PROJECTS, R.string.nav_projects, Icons.Outlined.Cable, Icons.Filled.Cable),
    Workdays(Routes.WORKDAYS, R.string.nav_workdays, Icons.Outlined.EditNote, Icons.Filled.EditNote),
    Expenses(Routes.EXPENSES, R.string.nav_expenses, Icons.Outlined.Payments, Icons.Filled.Payments),
    Reports(Routes.REPORTS, R.string.nav_reports, Icons.Outlined.Insights, Icons.Filled.Insights),
    Settings(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Tune, Icons.Filled.Tune),
}

private val longArg = { name: String -> navArgument(name) { type = NavType.LongType } }

@Composable
fun ShootingApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { currentRoute == it.route }
    val showFab = currentRoute in listOf(Routes.DASHBOARD, Routes.WORKDAYS, Routes.REPORTS)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    windowInsets = NavigationBarDefaults.windowInsets,
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = backStackEntry?.destination?.hierarchy?.any {
                            it.route == destination.route
                        } ?: false
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.08f else 1f,
                            label = "navigationIconScale",
                        )
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.DASHBOARD) { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.icon,
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                )
                            },
                            label = {
                                Text(
                                    stringResource(destination.labelRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.workdayEditor(0L, forceNew = false)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.workday_new))
                }
            }
        },
    ) { innerPadding ->
        val swipeOffset = remember { Animatable(0f) }
        val swipeScope = rememberCoroutineScope()
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = swipeOffset.value
                    val fraction = if (size.width > 0f) abs(swipeOffset.value) / size.width else 0f
                    scaleX = 1f - (fraction * 0.025f)
                }
                .pointerInput(currentRoute) {
                    if (TopLevelDestination.entries.any { it.route == currentRoute }) {
                        var drag = 0f
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                drag += amount
                                val destinations = TopLevelDestination.entries
                                val index = destinations.indexOfFirst { it.route == currentRoute }
                                val atEdge = (drag > 0f && index == 0) ||
                                    (drag < 0f && index == destinations.lastIndex)
                                val visualOffset = if (atEdge) {
                                    val limit = size.width * 0.22f
                                    sign(drag) * limit * (1f - exp(-abs(drag) / (size.width.coerceAtLeast(1) * 0.55f)))
                                } else drag.coerceIn(-size.width.toFloat(), size.width.toFloat())
                                swipeScope.launch { swipeOffset.snapTo(visualOffset) }
                            },
                            onDragEnd = {
                                val destinations = TopLevelDestination.entries
                                val index = destinations.indexOfFirst { it.route == currentRoute }
                                val target = when {
                                    drag < -size.width * 0.22f && index >= 0 && index < destinations.lastIndex -> destinations[index + 1]
                                    drag > size.width * 0.22f && index > 0 -> destinations[index - 1]
                                    else -> null
                                }
                                target?.let { destination ->
                                    navController.navigate(destination.route) {
                                        popUpTo(Routes.DASHBOARD) { inclusive = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                                swipeScope.launch { swipeOffset.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 420f)) }
                                drag = 0f
                            },
                            onDragCancel = {
                                swipeScope.launch { swipeOffset.animateTo(0f, spring()) }
                                drag = 0f
                            },
                        )
                    }
                },
            color = MaterialTheme.colorScheme.background,
        ) {
            ShootingNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
fun ShootingNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNewWorkday = { navController.navigate(Routes.workdayEditor(0L)) },
                onOpenWorkday = { id -> navController.navigate(Routes.workdayDetail(id)) },
                onOpenReports = { navController.navigate(Routes.REPORTS) },
                onOpenOperations = { navController.navigate(Routes.WORKDAYS) },
            )
        }
        composable(Routes.PROJECTS) {
            ProjectsScreen(
                onNewProject = { navController.navigate(Routes.projectEditor(0L)) },
                onEditProject = { id -> navController.navigate(Routes.projectEditor(id)) },
                onOpenProjectWorkdays = { navController.navigate(Routes.WORKDAYS) },
            )
        }
        composable(
            Routes.PROJECT_EDITOR,
            arguments = listOf(longArg("projectId")),
        ) { entry ->
            ProjectEditorScreen(
                projectId = entry.arguments?.getLong("projectId") ?: 0L,
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.WORKDAYS) {
            WorkdaysScreen(
                onNewWorkday = { navController.navigate(Routes.workdayEditor(0L)) },
                onOpenWorkday = { id -> navController.navigate(Routes.workdayDetail(id)) },
            )
        }
        composable(
            Routes.WORKDAY_EDITOR,
            arguments = listOf(longArg("workdayId"), longArg("projectId"), navArgument("forceNew") { type = NavType.BoolType; defaultValue = true }),
        ) { entry ->
            WorkdayEditorScreen(
                workdayId = entry.arguments?.getLong("workdayId") ?: 0L,
                initialProjectId = entry.arguments?.getLong("projectId") ?: 0L,
                forceNew = entry.arguments?.getBoolean("forceNew") ?: true,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            Routes.WORKDAY_DETAIL,
            arguments = listOf(longArg("workdayId")),
        ) { entry ->
            WorkdayDetailScreen(
                workdayId = entry.arguments?.getLong("workdayId") ?: 0L,
                onEdit = { id -> navController.navigate(Routes.workdayEditor(id)) },
                onBack = { navController.popBackStack() },
                onNavigateWorkday = { id ->
                    navController.navigate(Routes.workdayDetail(id)) {
                        popUpTo(Routes.WORKDAY_DETAIL) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.EXPENSES) {
            ExpensesScreen(
                onNewExpense = { navController.navigate(Routes.expenseEditor(0L)) },
                onEditExpense = { id -> navController.navigate(Routes.expenseEditor(id)) },
            )
        }
        composable(
            Routes.EXPENSE_EDITOR,
            arguments = listOf(longArg("expenseId")),
        ) { entry ->
            ExpenseEditorScreen(
                expenseId = entry.arguments?.getLong("expenseId") ?: 0L,
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.REPORTS) { ReportsScreen() }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
                onOpenReferences = { navController.navigate(Routes.REFERENCES) },
            )
        }
        composable(Routes.BACKUP) { BackupScreen() }
        composable(Routes.REFERENCES) { ReferencesScreen() }
    }
}
