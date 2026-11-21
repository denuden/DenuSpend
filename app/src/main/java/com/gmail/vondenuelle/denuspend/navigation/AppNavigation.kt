package com.gmail.vondenuelle.denuspend.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: RootGraphs,
    onNavigateToTab: (NavigationScreens) -> Unit,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        addMainNavGraph(navController, onNavigateToTab)
        addProfileGraph(navController)
        addNavGraph(navController)
        addBudgetNavGraph(navController)
        addWalletNavGraph(navController)
    }
}
