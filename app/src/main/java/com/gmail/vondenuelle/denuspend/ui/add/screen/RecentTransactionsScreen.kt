package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ModalBottomSheet
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionsForDayRequest
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.ui.add.components.DailyHistoryItem
import com.gmail.vondenuelle.denuspend.ui.common.components.PullToRefreshCustomStyle
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.common.skeleton.SkeletonTransactionList
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import com.gmail.vondenuelle.denuspend.utils.formatFirebaseDate
import kotlinx.coroutines.launch

@Composable
fun RecentTransactions(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var shouldShowTransactionDialog by remember { mutableStateOf(false) }

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

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(AddScreenEvents.OnGetDailyTransactionHistory)
            }

            Lifecycle.Event.ON_STOP -> {
                viewModel.stopOverviewListener()
            }

            else -> Unit
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

    ModalBottomSheetDialog(
        showDialog = shouldShowTransactionDialog,
        onDismissRequest = {
            shouldShowTransactionDialog = false
            viewModel.stopPerDayListener()
        },
    ) {
        TransactionDialogContent(
            modifier = Modifier.fillMaxWidth(),
            list = state.perDayTransactionList
        )
    }
    RecentTransactionsContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onEvent = viewModel::onEvent,
        openDialog = {
            shouldShowTransactionDialog = it
        }
    )
}

@Composable
fun TransactionDialogContent(modifier: Modifier = Modifier, list: List<TransactionModel>) {
    LazyColumn(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        item {
            Text("Last Added", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(list) {
            TransactionItem(transactionModel = it)
        }
        if (list.isEmpty()) {
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

@Composable
fun RecentTransactionsContent(
    modifier: Modifier = Modifier,
    onEvent: (AddScreenEvents) -> Unit,
    state: AddScreenState,
    openDialog : (Boolean) -> Unit
) {
    PullToRefreshCustomStyle(
        isRefreshing = state.isListLoading,
        onRefresh = {
            onEvent(AddScreenEvents.OnGetDailyTransactionHistory)
        }
    ) {
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = state.isListLoading
        ) {
            Column(
                modifier = Modifier
            ) {
                repeat(5) {
                    SkeletonTransactionList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isListLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LazyColumn(
                modifier = modifier
                    .padding(16.dp)
            ) {
                items(state.dailyHistoryList) {
                    val expense = it.totalExpense
                    val income = it.totalIncome

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
                    DailyHistoryItem(
                        modifier = Modifier.fillMaxWidth(),
                        expense = "₱${CurrencyUtils.formatCents(expense)}",
                        totalBudget = "₱${CurrencyUtils.formatCents(income)}",
                        progress = animatedProgress,
                        date = formatFirebaseDate(it.date.toDate()),
                        onClick = {
                            openDialog(true)
                            onEvent(AddScreenEvents.OnGetTransactionsForDay(
                                TransactionsForDayRequest(dailyDocId = it.docId, limit = null)))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (state.dailyHistoryList.isEmpty()) {
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
}

@Preview
@Composable
private fun RecentTransactionsPreview() {
    DenuSpendTheme {
        Surface {
            RecentTransactionsContent(
                modifier = Modifier.fillMaxSize(),
                state = AddScreenState(),
                onEvent = {},
                openDialog = {}
            )
        }
    }
}