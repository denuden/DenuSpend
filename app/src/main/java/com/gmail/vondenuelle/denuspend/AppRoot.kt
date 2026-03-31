package com.gmail.vondenuelle.denuspend

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.gmail.vondenuelle.denuspend.navigation.AppRootScreens
import com.gmail.vondenuelle.denuspend.navigation.AuthScreens
import com.gmail.vondenuelle.denuspend.navigation.addAuthNavGraph

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AppRootScreens.SplashTopLevel
    ) {
        composable<AppRootScreens.SplashTopLevel> {

            SplashScreen(
                onNavigate = { route,  options ->
                    navController.navigate(route, options)
                },
            )
        }

        addAuthNavGraph(navController)

        composable<AppRootScreens.MainTopLevel>  { backStackEntry ->
            // Extract the argument using .toRoute()
            MainScreen(
                topLevelNavController = navController
            )
        }
    }
}
