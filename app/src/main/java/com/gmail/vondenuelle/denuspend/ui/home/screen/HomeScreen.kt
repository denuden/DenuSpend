package com.gmail.vondenuelle.denuspend.ui.home.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.navigation.MainScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.home.HomeScreenEvents
import com.gmail.vondenuelle.denuspend.ui.home.HomeScreenState
import com.gmail.vondenuelle.denuspend.ui.home.HomeViewmodel
import com.gmail.vondenuelle.denuspend.ui.home.components.BalanceCard
import com.gmail.vondenuelle.denuspend.ui.home.components.LastAddedCard
import com.gmail.vondenuelle.denuspend.ui.home.components.MonthCard
import com.gmail.vondenuelle.denuspend.ui.home.components.TodayCategoryChips
import com.gmail.vondenuelle.denuspend.ui.home.components.TodayLeftCard
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents

@Composable
fun HomeScreen(
    onNavigate : (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack : () -> Unit,
    onNavigateToTab : (NavigationScreens) -> Unit,
    viewModel : HomeViewmodel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.channel) { event ->
        when(event){
            is OneTimeEvents.OnNavigate -> {
                onNavigate(event.route, event.behavior.toNavOptions())
            }
            is OneTimeEvents.ShowError -> Toast.makeText(context, event.msg, Toast.LENGTH_SHORT).show()
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

//    ComposableLifecycle { owner, event ->
//       if (event == Lifecycle.Event.ON_START){
//           viewModel.onEvent(HomeScreenEvents.OnGetCurrentUser)
//       }
//    }

    // Home is a bottom-nav tab: no ON_STOP stop, so its listeners stay live while the tab is saved
    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            viewModel.onEvent(HomeScreenEvents.OnGetToday)
            viewModel.onEvent(HomeScreenEvents.OnGetMonth)
            viewModel.onEvent(HomeScreenEvents.OnGetWallets)
        }
    }

    HomeScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToTab = onNavigateToTab,
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    state : HomeScreenState,
    onEvent : (HomeScreenEvents) -> Unit,
    onNavigateToTab : (NavigationScreens) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier

    ) {
        // Read-only dashboard: every card only links to the screen that owns its data
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            TodayLeftCard(
                allowance = state.todayHistory.totalIncome,
                spent = state.todayHistory.totalExpense,
                isLoading = state.isTodayLoading,
                onClick = { onNavigateToTab(MainScreens.AddNavigation) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            // Edge to edge so the chips scroll under the screen's sides; the row adds its own 16dp padding
            TodayCategoryChips(
                allowanceByCategory = state.todayHistory.allowanceByCategory,
                expenseByCategory = state.todayHistory.expenseByCategory,
                onClick = { onNavigateToTab(MainScreens.AddNavigation) },
                modifier = Modifier.fillMaxWidth()
            )
            MonthCard(
                allowance = state.monthHistory.allowanceByCategory.values.sum(),
                spent = state.monthHistory.expenseByCategory.values.sum(),
                isLoading = state.isMonthLoading,
                onClick = { onNavigateToTab(MainScreens.BudgetNavigation) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            BalanceCard(
                wallets = state.wallets,
                dueCardCharges = state.dueCardCharges,
                isLoading = state.isWalletsLoading,
                onClick = { onNavigateToTab(MainScreens.WalletNavigation) },
                onWalletClick = { onEvent(HomeScreenEvents.OnOpenWalletDetails(it)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LastAddedCard(
                transactions = state.todayTransactions,
                isLoading = state.isTodayLoading,
                onClick = { onNavigateToTab(MainScreens.AddNavigation) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    DenuSpendTheme {
        Surface {
            HomeScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = HomeScreenState(
                    todayHistory = DailyHistoryModel(
                        totalIncome = 90000,
                        totalExpense = 51000,
                        allowanceByCategory = mapOf("Food" to 40000L, "Transportation" to 20000L, "Personal" to 30000L),
                        expenseByCategory = mapOf("Food" to 28000L, "Transportation" to 23000L),
                    ),
                    isTodayLoading = false,
                    isMonthLoading = false,
                    isWalletsLoading = false,
                ),
                onEvent = {},
                onNavigateToTab = {},
            )
        }
    }
}
