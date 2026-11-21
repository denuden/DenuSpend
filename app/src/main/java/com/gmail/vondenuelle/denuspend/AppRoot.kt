package com.gmail.vondenuelle.denuspend

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gmail.vondenuelle.denuspend.navigation.AppRootScreens
import com.gmail.vondenuelle.denuspend.navigation.AuthNavHost
import com.gmail.vondenuelle.denuspend.ui.wallet.screen.WalletSetupScreen

/**
 * Top level: Splash -> Auth, Wallet setup or Main. The session decides, this file is the only place that switches.
 * Each world is its own back stack entry, so popping it clears all ViewModels (and their Firestore listeners) inside it.
 */
@Composable
fun AppRoot(
    // Created outside the NavHost, so it lives as long as the Activity
    viewModel: MainViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AppRootScreens.SplashTopLevel) {
        composable<AppRootScreens.SplashTopLevel> {
            SplashScreen()
        }
        composable<AppRootScreens.AuthTopLevel> {
            AuthNavHost(onLoginOrRegisterCompleted = viewModel::onLoginOrRegisterCompleted)
        }
        composable<AppRootScreens.MainTopLevel> {
            MainScreen(viewModel = viewModel)
        }
        composable<AppRootScreens.WalletSetupTopLevel> {
            WalletSetupScreen(onSetupCompleted = viewModel::onWalletSetupCompleted)
        }
    }

    // Runs every time the session changes and moves to the matching world, removing everything before it.
    LaunchedEffect(session) {
        val target = when (session) {
            SessionState.Loading -> return@LaunchedEffect // stay on splash
            SessionState.LoggedOut -> AppRootScreens.AuthTopLevel
            SessionState.LoggedIn -> AppRootScreens.MainTopLevel
            SessionState.NeedsWalletSetup -> AppRootScreens.WalletSetupTopLevel
        }
        // Already there (e.g. after rotation), don't recreate it
        if (navController.currentDestination?.hasRoute(target::class) == true) return@LaunchedEffect

        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
}
