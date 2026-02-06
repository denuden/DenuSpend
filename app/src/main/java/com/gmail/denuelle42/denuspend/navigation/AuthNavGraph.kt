package com.gmail.denuelle42.denuspend.navigation
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gmail.denuelle42.denuspend.ui.auth.LoginScreen

fun NavGraphBuilder.addAuthNavGraph(
    navController: NavController
) {
    navigation<RootGraphs.AuthGraph>(startDestination = AuthScreens.LoginNavigation) {
        composable<AuthScreens.LoginNavigation> {
            LoginScreen(
                onNavigate = {
                    navController.navigate(it)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}