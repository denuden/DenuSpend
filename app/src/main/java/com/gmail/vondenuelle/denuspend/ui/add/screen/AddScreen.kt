package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.ui.add.components.section.AddButtonsSection
import com.gmail.vondenuelle.denuspend.ui.add.components.section.TodayIncomeAndExpensesSection
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

@Composable
fun AddScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }


    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> {
                onPopBackStack()
            }

            is OneTimeEvents.OnNavigate -> {
                val options = NavOptions.Builder().apply {
                    when (event.behavior) {
                        NavBehavior.ClearAll -> {
                            setPopUpTo(0, inclusive = true)
                            setLaunchSingleTop(true)
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

    ComposableLifecycle(lifecycle) { _, event ->
        if(event == Lifecycle.Event.ON_START ) {
            viewModel.setTransactionLimit(null)
            viewModel.onEvent(AddScreenEvents.OnGetSummaryOfDailyTransactions)
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
    AddScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun AddScreenContent(
    modifier: Modifier = Modifier,
    state: AddScreenState,
    onEvent: (AddScreenEvents) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        val expense = -state.transactionSummary.totalExpense
        val income = state.transactionSummary.totalIncome

        val progress: Float = if (income > 0) {
            expense.toFloat() / income.toFloat()
        } else {
            0f // avoid division by zero
        }

        // Animate the progress value
        val animatedProgress by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f), // keep within [0,1]
            animationSpec = tween(
                durationMillis = 800, // adjust speed
                easing = FastOutSlowInEasing // smooth curve
            )
        )

        TodayIncomeAndExpensesSection(
            modifier = Modifier.fillMaxWidth(),
            progress = animatedProgress,
            expense =   "₱${CurrencyUtils.formatCents(expense)}",
            totalBudget = "₱${CurrencyUtils.formatCents(income)}",
        )
        Spacer(modifier = Modifier.height(8.dp))

        AddButtonsSection(modifier = Modifier.fillMaxWidth(), onExpenseClick = {
            onEvent(AddScreenEvents.OnNavigateToExpenseScreen)

        },  onIncomeClick= {
            onEvent(AddScreenEvents.OnNavigateToIncomeScreen)
        })

        Spacer(modifier = Modifier.height(8.dp))


        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Last Added", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
               contentPadding = PaddingValues(0.dp),
                onClick = {
                    onEvent(AddScreenEvents.OnNavigateToRecentTransactions)
                }
            ) {
                Text("History", fontWeight = FontWeight.Medium)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(state.transactionList) {
                TransactionItem(transactionModel = it)
            }
            if(state.transactionList.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Nothing to show", fontWeight = FontWeight.W300)
                    }
                }
            }

        }
    }

}

@Preview
@Composable
private fun AddScreenPreview() {
    DenuSpendTheme {
        Surface {
            AddScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = AddScreenState(),
                onEvent = {})
        }
    }
}