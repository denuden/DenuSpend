package com.gmail.vondenuelle.denuspend.ui.budget.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenEvents
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenState
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetViewModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.AnalysisCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.MiniAnalysisCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.PerDayAnalysisCard
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

@Composable
fun BudgetTransactionScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> {
                onPopBackStack()
            }

            is OneTimeEvents.OnNavigate -> {
                val options = NavOptions.Builder().apply {
                    when (event.behavior) {
                        is NavBehavior.ClearAll -> {
                            setPopUpTo(0, inclusive = true)
                            setLaunchSingleTop(true)
                            setRestoreState(false)
                        }

                        is NavBehavior.PopUpTo -> {
                            setPopUpTo(
                                event.behavior.destination,
                                inclusive = event.behavior.inclusive
                            )
                        }

                        NavBehavior.None -> Unit
                    }
                }.build()

                onNavigate(event.route, options)
            }

            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT)
                .show()

            is OneTimeEvents.ShowSnackbar -> {
                scope.launch {
                    SnackBarController.sendEvent(event.snackbarEvent)
                }
            }

            OneTimeEvents.OnCloseDialog -> {}
            is OneTimeEvents.ShowInputError -> {}
        }
    }


    LoadingDialog(
        showDialog = state.isLoading,
        text = "Loading...",
    ) { }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    BudgetTransactionScreenContent(state = state, onEvent = viewModel::onEvent,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp))

}

@Composable
fun BudgetTransactionScreenContent(
    modifier: Modifier = Modifier,
    state : BudgetScreenState,
    onEvent : (BudgetScreenEvents) -> Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            MiniAnalysisCard(){}
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            AnalysisCard(title = "Food Analysis") { monthIndex, year -> }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PerDayAnalysisCard(title = "Jan, 2025", numberOfDays = 31) { date -> }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Selected date spending breakdown",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Light,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                AssistChip(
                    onClick = {
                        //Todo
                    },
                    label = {
                        Text("01 Jan, 2025")
                    },
                    trailingIcon = {
                        Icon(Icons.Filled.CalendarMonth, null)
                    },
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                )
            }
        }
        items(5) {
            Spacer(modifier = Modifier.height(8.dp))
            TransactionItem(
                transactionModel = TransactionModel(
                    title = "Starbucks",
                    description = "Matcha Latte",
                    amount = 25000,
                    category = "Food"
                )
            )
        }

    }
}


@Preview
@Composable
private fun BudgetTransactionScreenPreview() {
    DenuSpendTheme() {
        Surface {
            BudgetTransactionScreenContent(state = BudgetScreenState()) {}
        }
    }


    
}