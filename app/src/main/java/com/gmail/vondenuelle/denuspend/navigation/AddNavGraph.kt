package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.gmail.vondenuelle.denuspend.ui.add.screen.AddExpenseScreen
import com.gmail.vondenuelle.denuspend.ui.add.screen.AddIncomeScreen
import com.gmail.vondenuelle.denuspend.ui.add.screen.RecentTransactions

fun NavGraphBuilder.addNavGraph(
    navController: NavController,
    topLevelNavController : NavController,
) {
    navigation<RootGraphs.AddGraph>(startDestination = AddScreens.AddIncomeScreenNavigation){
        composable<AddScreens.AddIncomeScreenNavigation> {
            AddIncomeScreen(
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
        composable<AddScreens.AddExpenseScreenNavigation> {
            AddExpenseScreen(
                onNavigate = { route, navOptions ->
                    if (route == AppRootScreens.AuthTopLevel) {
                        topLevelNavController.navigate(AppRootScreens.AuthTopLevel) {
                            popUpTo(AppRootScreens.MainTopLevel) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
        composable<AddScreens.AllRecentTransactionsNavigation> {
            RecentTransactions(
                onNavigate = { route, navOptions ->
                    if (route == AppRootScreens.AuthTopLevel) {
                        topLevelNavController.navigate(AppRootScreens.AuthTopLevel) {
                            popUpTo(AppRootScreens.MainTopLevel) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}