package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.gmail.vondenuelle.denuspend.ui.home.screen.HomeScreen

fun NavGraphBuilder.addMainNavGraph(
    navController: NavController
) {
    navigation<RootGraphs.MainGraph>(startDestination = MainScreens.HomeNavigation) {
        composable<MainScreens.HomeNavigation> {
            HomeScreen(
                onNavigate = { route, navOptions ->
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}