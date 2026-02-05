package com.gmail.denuelle42.denuspend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gmail.denuelle42.denuspend.navigation.AppNavigation
import com.gmail.denuelle42.denuspend.navigation.NavigationScreens
import com.gmail.denuelle42.denuspend.navigation.RootGraphs
import com.gmail.denuelle42.denuspend.navigation.getTopBarTitle
import com.gmail.denuelle42.denuspend.ui.theme.DenuSpendTheme
import com.gmail.denuelle42.denuspend.utils.ObserveAsEvents
import com.gmail.denuelle42.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(isLoggedIn: Boolean) {
    val navController = rememberNavController()

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Checks current type to determine which component should be shown or not from the scaffold
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screenType =  "MainScreens"

    // holds state if topborcontent should be shown
    var topBarState by rememberSaveable { (mutableStateOf(false)) }

    // holds title of top bar
    var topBarTitle by remember { mutableStateOf("") }

    //detects current route changes, then set topbarstate
    //if visible depending on route
    LaunchedEffect(currentRoute) {
        //will show topbarcontent if route is from mainscreens (E.G. Home)
        topBarState = currentRoute?.contains(screenType) == true
        topBarTitle = getTopBarTitle(currentRoute.toString())
    }

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
                duration = SnackbarDuration.Long
            )

            if (result == SnackbarResult.ActionPerformed) {
                event.action?.action?.invoke()
            }
        }
    }
    ModalNavigationDrawer(
        gesturesEnabled = topBarState, //only enable gestures if topbar is visible
        drawerState = drawerState,
        drawerContent = {
        }
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState
                )
            },
            topBar = {
                TopAppBarContent(
                    title = topBarTitle,
                    onClickNavigationMenu = {
                        scope.launch {
                            drawerState.apply {
                                if (isClosed) open() else close()
                            }
                        }
                    },
                    topBarState = topBarState,
                    onPopBackStack = {
                        navController.popBackStack()
                    },
                    onNavigate = {
                    }
                )
            },
        ) { contentPadding ->
            // Screen content
            Box(modifier = Modifier.padding(contentPadding)) {
                AppNavigation(
                    navController,
                    startDestination = if (isLoggedIn) RootGraphs.HomeGraph else RootGraphs.AuthGraph)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarContent(
    modifier: Modifier = Modifier,
    onClickNavigationMenu: () -> Unit,
    topBarState: Boolean,
    title: String,
    onPopBackStack: () -> Unit,
    onNavigate: (NavigationScreens) -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(title)
        },
        navigationIcon = {
            if (topBarState) {
                IconButton(onClick = onClickNavigationMenu) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu Bar"
                    )
                }
            } else {
                IconButton(
                    onClick = onPopBackStack,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                    )
                }
            }
        },

        actions = {
            if (topBarState) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null
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
    DenuSpendTheme  {
        Surface(
            color = MaterialTheme.colorScheme.surface,
        ) {
            TopAppBarContent(onClickNavigationMenu = {

            }, topBarState = false, onPopBackStack = {}, title = "", onNavigate = {})
        }
    }
}