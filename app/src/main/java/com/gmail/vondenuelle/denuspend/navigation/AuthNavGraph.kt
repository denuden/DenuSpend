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
                onNavigate = {
                    navController.navigate(route = it, navOptions = NavOptions.Builder()
                        .setPopUpTo<AuthScreens.LoginNavigation>(inclusive = true)
                        .setLaunchSingleTop(true)
                        .build())
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
        composable<AuthScreens.RegisterNavigation> {
            RegisterScreen(
                onNavigate = {
                    navController.navigate(route = it, navOptions = NavOptions.Builder()
                        .setPopUpTo<AuthScreens.RegisterNavigation>(inclusive = true)
                        .setLaunchSingleTop(true)
                        .build())
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}