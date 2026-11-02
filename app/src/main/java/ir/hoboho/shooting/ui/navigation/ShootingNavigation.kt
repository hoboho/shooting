package ir.hoboho.shooting.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    const val WORKDAY_EDITOR = "workday_editor/{workdayId}/{projectId}"
    const val WORKDAY_DETAIL = "workday_detail/{workdayId}"
    const val EXPENSE_EDITOR = "expense_editor/{expenseId}"

    fun projectEditor(id: Long) = "project_editor/$id"
    fun workdayEditor(workdayId: Long, projectId: Long = 0L) = "workday_editor/$workdayId/$projectId"
    fun workdayDetail(id: Long) = "workday_detail/$id"
    fun expenseEditor(id: Long) = "expense_editor/$id"
}

enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Dashboard(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Home),
    Projects(Routes.PROJECTS, R.string.nav_projects, Icons.Filled.Business),
    Workdays(Routes.WORKDAYS, R.string.nav_workdays, Icons.Filled.ListAlt),
    Expenses(Routes.EXPENSES, R.string.nav_expenses, Icons.Filled.Receipt),
    Reports(Routes.REPORTS, R.string.nav_reports, Icons.Filled.Assessment),
    Settings(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
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
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
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
                    onClick = { navController.navigate(Routes.workdayEditor(0L)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.workday_new))
                }
            }
        },
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
                onNewExpense = { navController.navigate(Routes.expenseEditor(0L)) },
                onOpenReports = { navController.navigate(Routes.REPORTS) },
                onOpenProjects = { navController.navigate(Routes.PROJECTS) },
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
            arguments = listOf(longArg("workdayId"), longArg("projectId")),
        ) { entry ->
            WorkdayEditorScreen(
                workdayId = entry.arguments?.getLong("workdayId") ?: 0L,
                initialProjectId = entry.arguments?.getLong("projectId") ?: 0L,
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
