package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.gmail.vondenuelle.denuspend.ui.home.screen.HomeScreen
import com.gmail.vondenuelle.denuspend.ui.profile.screen.ProfileScreen

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
        composable<MainScreens.ProfileNavigation> {
            ProfileScreen (
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