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
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.ui.add.components.DailyHistoryItem
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionActionsSheet
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.gmail.vondenuelle.denuspend.R
import kotlin.math.abs

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

    // The day the sheet is showing; null means the sheet is closed.
    var selectedDay by remember { mutableStateOf<DailyHistoryModel?>(null) }
    // The transaction tapped inside the day sheet; null means the actions sheet is closed.
    var selectedTransaction by remember { mutableStateOf<TransactionModel?>(null) }

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> {
                onPopBackStack()
            }

            is OneTimeEvents.OnNavigate -> {
                onNavigate(event.route, event.behavior.toNavOptions())
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
            OneTimeEvents.OnLoginOrRegisterCompleted -> {}
            is OneTimeEvents.ShowInputError -> {}
        }
    }

    // Rotation closes the sheet without onDismissRequest, so stop the per-day listener when this screen leaves composition.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopPerDayListener() }
    }

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(AddScreenEvents.OnGetDailyTransactionHistory)
            }

            Lifecycle.Event.ON_STOP -> {
                viewModel.stopHistoryListener()
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
        showDialog = selectedDay != null,
        onDismissRequest = {
            selectedDay = null
            viewModel.stopPerDayListener()
        },
    ) {
        selectedDay?.let { selected ->
            // Read the live copy so the header totals update while the sheet is open.
            val day = state.dailyHistoryList.find { it.docId == selected.docId } ?: selected
            TransactionDialogContent(
                modifier = Modifier.fillMaxWidth(),
                day = day,
                list = state.perDayTransactionList,
                isLoading = state.isSheetLoading,
                onTransactionClick = { selectedTransaction = it }
            )
        }
    }

    // Opens on top of the day sheet; the day sheet's live listener shows a delete right away.
    ModalBottomSheetDialog(
        showDialog = selectedTransaction != null,
        onDismissRequest = { selectedTransaction = null },
    ) {
        val day = selectedDay
        selectedTransaction?.let { selected ->
            if (day == null) return@let
            TransactionActionsSheet(
                modifier = Modifier.fillMaxWidth(),
                transaction = selected,
                onEdit = {
                    // Close both sheets before leaving; the per-day listener isn't needed on the form
                    selectedTransaction = null
                    selectedDay = null
                    viewModel.stopPerDayListener()
                    viewModel.onEvent(AddScreenEvents.OnNavigateToEditTransaction(selected, day.docId))
                },
                onDelete = {
                    selectedTransaction = null
                    viewModel.onEvent(AddScreenEvents.OnDeleteTransaction(selected, day.docId))
                }
            )
        }
    }
    RecentTransactionsContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onEvent = viewModel::onEvent,
        onDayClick = { day ->
            selectedDay = day
            viewModel.onEvent(
                AddScreenEvents.OnGetTransactionsForDay(
                    TransactionsForDayRequest(dailyDocId = day.docId, limit = null)
                )
            )
        }
    )
}

@Composable
fun TransactionDialogContent(
    modifier: Modifier = Modifier,
    day: DailyHistoryModel,
    list: List<TransactionModel>,
    isLoading: Boolean,
    onTransactionClick: (TransactionModel) -> Unit = {},
) {
    LazyColumn(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        item {
            val isOverspent = day.totalExpense > day.totalIncome
            Text(
                formatFirebaseDate(day.date.toDate(), format = "EEEE, MMMM dd, yyyy"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)) {
                DayStat(
                    label = "Income",
                    value = "₱${CurrencyUtils.formatCents(day.totalIncome)}",
                    color = colorResource(R.color.verified_green_text),
                    modifier = Modifier.weight(1f)
                )
                DayStat(
                    label = "Expense",
                    value = "₱${CurrencyUtils.formatCents(day.totalExpense)}",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                DayStat(
                    label = if (isOverspent) "Over" else "Left",
                    value = "₱${CurrencyUtils.formatCents(abs(day.totalIncome - day.totalExpense))}",
                    color = if (isOverspent) MaterialTheme.colorScheme.error else colorResource(R.color.verified_green_text),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (isLoading) {
            items(3) {
                SkeletonTransactionList(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
        items(list) {
            TransactionItem(transactionModel = it, onClick = { onTransactionClick(it) })
        }
        if (list.isEmpty() && !isLoading) {
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
private fun DayStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun MonthHeader(month: String, totalExpense: Long) {
    // Outer solid background so the cards scroll underneath the sticky header instead of showing through.
    // Inner tonal pill makes the header stand out from the card text.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            month,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        Text(
            "₱${CurrencyUtils.formatCents(totalExpense)} spent",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// "₱3,800.00 left" when under income, "₱200.00 over" when expense is higher.
private fun remainingLabel(income: Long, expense: Long): String =
    if (expense > income) "₱${CurrencyUtils.formatCents(expense - income)} over"
    else "₱${CurrencyUtils.formatCents(income - expense)} left"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentTransactionsContent(
    modifier: Modifier = Modifier,
    onEvent: (AddScreenEvents) -> Unit,
    state: AddScreenState,
    onDayClick : (DailyHistoryModel) -> Unit
) {
    val listState = rememberLazyListState()

    // Prefetch: ask for the next page once the user is within 5 items of the end.
    // The item count is part of the key so it re-checks after each page arrives.
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            (info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 5) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (isNearEnd, _) -> isNearEnd }
            .collect { onEvent(AddScreenEvents.OnLoadMoreHistory) }
    }

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
            // Group the loaded days by month for the sticky headers. groupBy keeps the newest-first order.
            val daysByMonth = remember(state.dailyHistoryList) {
                state.dailyHistoryList.groupBy { formatFirebaseDate(it.date.toDate(), format = "MMMM yyyy") }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = modifier
            ) {
                daysByMonth.forEach { (month, days) ->
                    stickyHeader(key = "header_$month") {
                        MonthHeader(
                            month = month,
                            totalExpense = days.sumOf { it.totalExpense }
                        )
                    }

                    items(days, key = { it.docId }) {
                        val expense = it.totalExpense
                        val income = it.totalIncome

                        val progress: Float = when {
                            income > 0 -> expense.toFloat() / income.toFloat()
                            expense > 0 -> 1f // spent with no income yet, show as full
                            else -> 0f // avoid division by zero
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
                            date = formatFirebaseDate(it.date.toDate(), format = "EEE, MMM dd"),
                            expense = "₱${CurrencyUtils.formatCents(expense)}",
                            totalBudget = "₱${CurrencyUtils.formatCents(income)}",
                            remaining = remainingLabel(income, expense),
                            progress = animatedProgress,
                            isOverspent = expense > income,
                            onClick = { onDayClick(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (state.isLoadingMoreHistory) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
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
                onDayClick = {}
            )
        }
    }
}