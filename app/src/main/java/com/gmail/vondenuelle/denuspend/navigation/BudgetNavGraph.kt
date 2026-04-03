package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gmail.vondenuelle.denuspend.ui.budget.screen.BudgetTransactionScreen

fun NavGraphBuilder.addBudgetNavGraph(
    navController: NavController,
    topLevelNavController : NavController,
) {
    navigation<RootGraphs.BudgetGraph>(startDestination = BudgetScreens.BudgetTransactionScreenNavigation){
        composable<BudgetScreens.BudgetTransactionScreenNavigation> {
            BudgetTransactionScreen(
                onNavigate = { route, navOptions ->
                    if (route == AppRootScreens.AuthTopLevel) {
                        topLevelNavController.navigate(AppRootScreens.AuthTopLevel) {
                            popUpTo(AppRootScreens.MainTopLevel) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    } else {
                        navController.navigate(route, navOptions = navOptions)
                    }
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}