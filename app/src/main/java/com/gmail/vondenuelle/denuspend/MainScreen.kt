package com.gmail.vondenuelle.denuspend

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.gmail.vondenuelle.denuspend.navigation.AppNavigation
import com.gmail.vondenuelle.denuspend.navigation.RootGraphs
import com.gmail.vondenuelle.denuspend.navigation.getTopBarTitle
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(isLoggedIn: Boolean) {
    val navController = rememberNavController()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Checks current type to determine which component should be shown or not from the scaffold
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val screenType =  "MainScreens"

    // holds state if topborcontent should be shown
    var topBarState by rememberSaveable { (mutableStateOf(false)) }
    // holds state if bottombar should be shown
    var bottomBarState by rememberSaveable { (mutableStateOf(false)) }

    // holds title of top bar
    var topBarTitle by remember { mutableStateOf("") }

    //detects current route changes, then set topbarstate
    //if visible depending on route
    LaunchedEffect(currentRoute) {
        //will show topbarcontent if route is from mainscreens (E.G. Home)
        topBarState = currentRoute?.contains(screenType) == true
        bottomBarState = currentRoute?.contains(screenType) == true
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

    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf("Home", "Wallet", "Add", "Goals", "Savings")
    val unselectedIcons =
        listOf(Icons.Outlined.Home, Icons.Outlined.AccountBalanceWallet, Icons.Outlined.AddCircleOutline, Icons.Outlined.Flag, Icons.Outlined.Savings)
        val selectedIcons = listOf(Icons.Filled.Home, Icons.Filled.AccountBalanceWallet, Icons.Filled.AddCircle, Icons.Filled.Flag, Icons.Filled.Savings)

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
        topBar = {
            if(topBarState) {
                TopAppBarContent(
                    title = topBarTitle,
                    onPopBackStack = {
                        navController.popBackStack()
                    },
                )
            }

        },
        bottomBar = {
            if(bottomBarState){
                NavigationBar(
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                    contentDescription = item,
                                )
                            },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
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
                startDestination = if (isLoggedIn) RootGraphs.MainGraph else RootGraphs.AuthGraph)
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarContent(
    modifier: Modifier = Modifier,
    title: String,
    onPopBackStack: () -> Unit,
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
            IconButton(
                onClick = onPopBackStack,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                )
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
            TopAppBarContent(onPopBackStack = {}, title = "")
        }
    }
}