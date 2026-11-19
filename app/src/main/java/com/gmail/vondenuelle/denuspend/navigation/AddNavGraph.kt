package com.gmail.vondenuelle.denuspend.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.gmail.vondenuelle.denuspend.ui.add.screen.AddExpenseScreen
import com.gmail.vondenuelle.denuspend.ui.add.screen.AddIncomeScreen
import com.gmail.vondenuelle.denuspend.ui.add.screen.RecentTransactions

fun NavGraphBuilder.addNavGraph(
    navController: NavController,
) {
    navigation<RootGraphs.AddGraph>(startDestination = AddScreens.AddIncomeScreenNavigation()){
        composable<AddScreens.AddIncomeScreenNavigation> { backStackEntry ->
            val args = backStackEntry.toRoute<AddScreens.AddIncomeScreenNavigation>()
            AddIncomeScreen(
                docId = args.docId,
                dailyDocId = args.dailyDocId,
                onNavigate = { route, navOptions ->
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
        composable<AddScreens.AddExpenseScreenNavigation> { backStackEntry ->
            val args = backStackEntry.toRoute<AddScreens.AddExpenseScreenNavigation>()
            AddExpenseScreen(
                docId = args.docId,
                dailyDocId = args.dailyDocId,
                onNavigate = { route, navOptions ->
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
                    navController.navigate(route, navOptions = navOptions)
                },
                onPopBackStack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
