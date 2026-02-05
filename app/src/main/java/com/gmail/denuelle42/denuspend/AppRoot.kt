package com.gmail.denuelle42.denuspend

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gmail.denuelle42.denuspend.navigation.AppRootScreens

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRootScreens.Splash
    ) {
        composable<AppRootScreens.Splash> {
            SplashScreen(
                onFinished = { isLoggedIn ->
                    navController.navigate(AppRootScreens.Main(isLoggedIn = isLoggedIn)) {
                        popUpTo<AppRootScreens.Splash> { inclusive = true }
                    }
                }
            )
        }

        composable<AppRootScreens.Main>  { backStackEntry ->
            // Extract the argument using .toRoute()
            val args = backStackEntry.toRoute<AppRootScreens.Main>()
            MainScreen(isLoggedIn = args.isLoggedIn)
        }
    }
}
