package com.gmail.vondenuelle.denuspend.navigation
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gmail.vondenuelle.denuspend.ui.auth.screen.LoginScreen
import com.gmail.vondenuelle.denuspend.ui.auth.screen.RegisterScreen

fun NavGraphBuilder.addAuthNavGraph(
    navController: NavController
) {
    navigation<RootGraphs.AuthGraph>(startDestination = AuthScreens.LoginNavigation) {
        composable<AuthScreens.LoginNavigation> {
            LoginScreen(
                onNavigate = {route, navOptions ->
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
        composable<AuthScreens.RegisterNavigation> {
            RegisterScreen(
                onNavigate = {route, navOptions ->
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}