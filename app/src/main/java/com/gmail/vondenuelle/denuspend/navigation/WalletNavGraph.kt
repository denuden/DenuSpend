package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.gmail.vondenuelle.denuspend.ui.wallet.screen.WalletDetailsScreen

fun NavGraphBuilder.addWalletNavGraph(
    navController: NavController,
) {
    navigation<RootGraphs.WalletGraph>(startDestination = WalletScreens.WalletDetailsNavigation("")) {
        composable<WalletScreens.WalletDetailsNavigation> {
            val args = it.toRoute<WalletScreens.WalletDetailsNavigation>()
            WalletDetailsScreen(
                walletId = args.walletId,
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
