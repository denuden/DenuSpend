package com.gmail.vondenuelle.denuspend.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.gmail.vondenuelle.denuspend.ui.profile.screen.ProfileScreen

fun NavGraphBuilder.addProfileGraph(
    navController: NavController
) {
    navigation<RootGraphs.ProfileGraph>(startDestination = ProfileScreens.ProfileNavigation) {
        composable<ProfileScreens.ProfileNavigation>(
        )
        {
            ProfileScreen(
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