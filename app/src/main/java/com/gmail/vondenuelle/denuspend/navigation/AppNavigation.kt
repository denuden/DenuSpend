package com.gmail.vondenuelle.denuspend.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: RootGraphs
) {
    NavHost(navController = navController, startDestination = startDestination) {
        addMainNavGraph(navController)
        addProfileGraph(navController)
        addNavGraph(navController)
        addBudgetNavGraph(navController)
    }
}
