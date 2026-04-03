package com.gmail.vondenuelle.denuspend.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

@Composable
fun AppNavigation(
    navController: NavHostController,
    topLevelNavController: NavHostController,
    startDestination: RootGraphs
) {
    NavHost(navController = navController, startDestination = startDestination) {
        addMainNavGraph(navController, topLevelNavController)
        addMainNavGraph(navController, topLevelNavController)
        addProfileGraph(navController, topLevelNavController)
        addNavGraph(navController, topLevelNavController)
        addBudgetNavGraph(navController, topLevelNavController)
    }
}




