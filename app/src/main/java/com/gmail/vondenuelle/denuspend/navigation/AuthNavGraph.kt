package com.gmail.vondenuelle.denuspend.navigation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gmail.vondenuelle.denuspend.ui.auth.screen.LoginScreen
import com.gmail.vondenuelle.denuspend.ui.auth.screen.RegisterScreen


fun NavGraphBuilder.addAuthNavGraph(
    navController: NavController
) {
    navigation<AppRootScreens.AuthTopLevel>(startDestination = AuthScreens.LoginNavigation) {

        val animationSpec = tween<IntOffset>(durationMillis = 400, easing = FastOutSlowInEasing)

        // Login Screen (slides to the LEFT when exiting)
        composable<AuthScreens.LoginNavigation>(
            enterTransition = {
                // Comes IN from the LEFT when returning from Register
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = animationSpec)
            },
            exitTransition = {
                // Goes OUT to the LEFT when navigating to Register
                slideOutHorizontally(targetOffsetX = { -it }, animationSpec = animationSpec)
            },
        ) {
            LoginScreen(
                onNavigate = { route, navOptions ->
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }

        // Register Screen (slides from the RIGHT)
        composable<AuthScreens.RegisterNavigation>(
            enterTransition = {
                // Comes IN from the RIGHT
                slideInHorizontally(initialOffsetX = { it }, animationSpec = animationSpec)
            },
            exitTransition = {
                // Goes OUT to the LEFT
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = animationSpec)
            },
        ) {
            RegisterScreen(
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
