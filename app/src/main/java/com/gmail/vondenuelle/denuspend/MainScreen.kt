package com.gmail.vondenuelle.denuspend

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gmail.vondenuelle.denuspend.navigation.AddScreens
import com.gmail.vondenuelle.denuspend.navigation.AppNavigation
import com.gmail.vondenuelle.denuspend.navigation.BudgetScreens
import com.gmail.vondenuelle.denuspend.navigation.MainScreens
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.ProfileScreens
import com.gmail.vondenuelle.denuspend.navigation.RootGraphs
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.AsyncImageWithErrorHandler
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

// route = null means the tab isn't built yet, so it's shown disabled
private data class BottomTab(
    val route: NavigationScreens?,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(MainScreens.HomeNavigation, R.string.main_tab_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomTab(null, R.string.main_tab_wallet, Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BottomTab(MainScreens.AddNavigation, R.string.main_tab_add, Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    BottomTab(null, R.string.main_tab_goals, Icons.Filled.Flag, Icons.Outlined.Flag),
    BottomTab(MainScreens.BudgetNavigation, R.string.main_tab_budget, Icons.Filled.Savings, Icons.Outlined.Savings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val user by viewModel.user.collectAsStateWithLifecycle()

    // Everything below is read from the back stack, so it's always in sync (Back button, rotation, etc.)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showTopBar = currentDestination.isIn<RootGraphs.MainGraph>() ||
            currentDestination.isIn<RootGraphs.AddGraph>() ||
            currentDestination.isIn<RootGraphs.BudgetGraph>()
    val showBottomBar = currentDestination.isIn<RootGraphs.MainGraph>()
    val canNavigateBack = navController.previousBackStackEntry != null

    //stating snackbar anywhere so it can be called from any screen
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    // observers snackbar controller events flow globally
    // snackbarhoststate as key to trigger launchedeffect block
    ObserveAsEvents(flow = SnackBarController.events, snackbarHostState) { event ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss() //dismiss ongoing snackbar
            val result = snackbarHostState.showSnackbar( // launch new snackbar
                message = event.message, actionLabel = event.action?.name,
                duration = SnackbarDuration.Short
            )

            if (result == SnackbarResult.ActionPerformed) {
                event.action?.action?.invoke()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        topBar = {
            if (showTopBar) {
                TopAppBarContent(
                    title = topBarTitle(currentDestination, user.name.orEmpty()),
                    canNavigateBack = canNavigateBack,
                    photo = user.photo.orEmpty(),
                    onPopBackStack = {
                        navController.popBackStack()
                    },
                    onNavigate = { route, navOptions ->
                        navController.navigate(route, navOptions = navOptions)
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = tab.route != null &&
                                currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.label),
                                )
                            },
                            label = { Text(stringResource(tab.label)) },
                            selected = selected,
                            enabled = tab.route != null,
                            onClick = {
                                // Standard bottom-nav options: keep one copy of each tab and remember each tab's state
                                tab.route?.let { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Screen content
        Box(modifier = Modifier.padding(padding)) {
            AppNavigation(
                navController,
                startDestination = RootGraphs.MainGraph,
            )
        }
    }
}

// True when the destination is inside the graph/screen T (type-safe, no route string matching)
private inline fun <reified T : Any> NavDestination?.isIn(): Boolean =
    this?.hierarchy?.any { it.hasRoute<T>() } == true

@Composable
private fun topBarTitle(destination: NavDestination?, name: String): String {
    val firstName = name.substringBefore(" ").ifEmpty { stringResource(R.string.main_default_name) }
    return when {
        destination == null -> ""
        destination.hasRoute<MainScreens.HomeNavigation>() -> stringResource(R.string.main_title_home, firstName)
        destination.hasRoute<MainScreens.AddNavigation>() -> stringResource(R.string.main_title_transaction)
        destination.hasRoute<MainScreens.BudgetNavigation>() -> stringResource(R.string.main_title_budget)
        destination.hasRoute<AddScreens.AddIncomeScreenNavigation>() -> stringResource(R.string.main_title_add_income)
        destination.hasRoute<AddScreens.AddExpenseScreenNavigation>() -> stringResource(R.string.main_title_add_expense)
        destination.hasRoute<AddScreens.AllRecentTransactionsNavigation>() -> stringResource(R.string.main_title_history)
        destination.hasRoute<BudgetScreens.BudgetInsightsScreenNavigation>() -> stringResource(R.string.main_title_budget_insights)
        destination.hasRoute<ProfileScreens.ProfileNavigation>() -> stringResource(R.string.main_title_profile)
        else -> stringResource(R.string.app_name)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarContent(
    modifier: Modifier = Modifier,
    title: String,
    photo: String,
    canNavigateBack: Boolean,
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
) {
    // Falls back to the default icon if the photo fails to load. Resets when the photo changes.
    var photoFailed by remember(photo) { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(title)
        },
        actions = {
            IconButton(
                onClick = {
                    onNavigate(ProfileScreens.ProfileNavigation, NavOptions.Builder().apply {
                        setLaunchSingleTop(true)
                    }.build())
                }
            ) {
                if (photo.isNotEmpty() && !photoFailed) {
                    AsyncImageWithErrorHandler(
                        model = photo.toUri(),
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        shouldShowEnlargeButton = false,
                        onError = {
                            photoFailed = true
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = stringResource(R.string.main_cd_profile)
                    )
                }
            }
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onPopBackStack,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.main_cd_back),
                    )
                }
            }
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun MainScreenPreview() {
    DenuSpendTheme {
        Surface(
            color = MaterialTheme.colorScheme.surface,
        ) {
            TopAppBarContent(
                onPopBackStack = {},
                title = "",
                photo = "",
                canNavigateBack = true,
                onNavigate = { _, _ -> })
        }
    }
}
