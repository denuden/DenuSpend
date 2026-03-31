package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.gmail.vondenuelle.denuspend.ui.profile.screen.ProfileScreen

fun NavGraphBuilder.addProfileGraph(
    navController: NavController,
    topLevelNavController: NavController,
) {
    navigation<RootGraphs.ProfileGraph>(startDestination = ProfileScreens.ProfileNavigation) {
        composable<ProfileScreens.ProfileNavigation>(
        )
        {
            ProfileScreen(
                onSignOut = {
                    topLevelNavController.navigate(AppRootScreens.AuthTopLevel) {
                        popUpTo(AppRootScreens.MainTopLevel) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
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