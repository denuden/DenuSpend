package com.gmail.vondenuelle.denuspend.ui.budget.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetInsightsModel
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenEvents
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenState
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetViewModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.InsightDay
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsCompareCard
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsChartCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsDaySheetContent
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsEmptyCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsWeekdayCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsHeader
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsMonthDetail
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsMonthsCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.dayBars
import com.gmail.vondenuelle.denuspend.ui.budget.components.monthBars
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsSentencesCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.InsightsSummaryCard
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.gmail.vondenuelle.denuspend.ui.budget.components.formatPickerRange
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetRangePickerDialog

@Composable
fun BudgetInsightsScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    category : String,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    // The day the sheet is showing; null means the sheet is closed.
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

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

    // Pushed screen (not a tab), so the listener stops on ON_STOP. Restarting the same query within 30 min only bills changes.
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(BudgetScreenEvents.OnGetBudgetInsights(category))
                if (selectedDate != null) viewModel.resumeInsightDayListener()
            }
            Lifecycle.Event.ON_STOP -> {
                viewModel.stopInsightsListener()
                viewModel.stopInsightsMonthListener()
                viewModel.stopInsightDayListener()
            }
            else -> Unit
        }
    }

    // Rotation closes the sheet without onDismissRequest, so stop the day listener when this screen leaves composition.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopInsightDayListener() }
    }

    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    ModalBottomSheetDialog(
        showDialog = selectedDate != null,
        onDismissRequest = {
            selectedDate = null
            viewModel.stopInsightDayListener()
        },
    ) {
        // Read the live copy so the totals update while the sheet is open. On long ranges the day comes from the opened month.
        val days = state.budgetInsights.days + state.insightsMonthDays?.days.orEmpty()
        days.find { it.date == selectedDate }?.let { day ->
            InsightsDaySheetContent(
                day = day,
                category = state.insightsCategory,
                transactions = state.insightDayTransactions,
                isLoading = state.isInsightDaySheetLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (state.showRangePicker) {
        BudgetRangePickerDialog(
            onDismiss = { viewModel.onEvent(BudgetScreenEvents.OnShowRangePicker(false)) },
            onConfirm = { start, end -> viewModel.onEvent(BudgetScreenEvents.OnCustomRangeSelected(start, end)) }
        )
    }

    BudgetInsightsScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        selectedDate = selectedDate,
        onDayClick = { day ->
            selectedDate = day.date
            viewModel.onEvent(BudgetScreenEvents.OnGetInsightDayTransactions(day.dailyDocId))
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun BudgetInsightsScreenContent(
    modifier: Modifier = Modifier,
    state : BudgetScreenState,
    onEvent : (BudgetScreenEvents) -> Unit,
    selectedDate : LocalDate? = null,
    onDayClick : (InsightDay) -> Unit = {},
) {
    // Before the first result arrives the model is empty, so show the category from the route right away
    val insights = state.budgetInsights.let { if (it.category.isEmpty()) it.copy(category = state.insightsCategory) else it }
    val filterLabel = if (state.filter == BudgetChartFilter.CUSTOM && state.customStartMillis != null) {
        formatPickerRange(state.customStartMillis, state.customEndMillis ?: state.customStartMillis)
    } else {
        stringResource(state.filter.label)
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Where the chart card starts inside the scrolling column, so a Month by month tap can scroll back to it
    var chartTop by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Thin loading bar like the dashboard. The slot keeps its height so nothing jumps.
        Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.Center) {
            if (state.isInsightsLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        InsightsHeader(
            insights = insights,
            filterLabel = filterLabel,
            onChangeFilter = { onEvent(BudgetScreenEvents.OnChangeFilter(it)) }
        )
        InsightsSummaryCard(
            insights = insights,
            modifier = Modifier.padding(top = 16.dp)
        )
        // Nothing to compare or explain without spending, so the empty card shows instead
        if (insights.spent > 0) {
            InsightsCompareCard(insights = insights, modifier = Modifier.padding(top = 12.dp))
            InsightsSentencesCard(insights = insights, modifier = Modifier.padding(top = 12.dp))

            val chartModifier = Modifier
                .padding(top = 12.dp)
                .onGloballyPositioned { chartTop = it.positionInParent().y.toInt() }
            val openMonth = state.insightsMonth
            when {
                // Short range: the range's own days
                !insights.isLongRange -> InsightsChartCard(
                    title = stringResource(R.string.insights_chart_per_day),
                    subtitle = stringResource(R.string.insights_chart_per_day_sub),
                    bars = dayBars(insights.days),
                    selectedIndex = insights.days.indexOfFirst { it.date == selectedDate }.takeIf { it >= 0 },
                    onBarClick = { onDayClick(insights.days[it]) },
                    allowanceAsSteps = true,
                    category = insights.category,
                    modifier = chartModifier
                )
                // Long range with a month opened: that month's days, read from its daily docs
                openMonth != null -> {
                    val monthDays = state.insightsMonthDays?.days.orEmpty()
                    InsightsChartCard(
                        title = stringResource(
                            R.string.insights_chart_month_days,
                            openMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        ),
                        subtitle = stringResource(R.string.insights_chart_per_day_sub),
                        bars = dayBars(monthDays),
                        selectedIndex = monthDays.indexOfFirst { it.date == selectedDate }.takeIf { it >= 0 },
                        onBarClick = { onDayClick(monthDays[it]) },
                        allowanceAsSteps = true,
                        category = insights.category,
                        isLoading = state.isInsightsMonthLoading,
                        onBack = { onEvent(BudgetScreenEvents.OnOpenInsightsMonth(null)) },
                        footer = { state.insightsMonthDays?.let { InsightsMonthDetail(monthDays = it) } },
                        modifier = chartModifier
                    )
                }
                // Long range: one bar per month, from the monthly docs
                else -> InsightsChartCard(
                    title = stringResource(R.string.insights_chart_per_month),
                    subtitle = stringResource(R.string.insights_chart_per_month_sub),
                    bars = monthBars(insights.months),
                    selectedIndex = null,
                    onBarClick = { onEvent(BudgetScreenEvents.OnOpenInsightsMonth(insights.months[it].month)) },
                    allowanceAsSteps = false,
                    category = insights.category,
                    modifier = chartModifier
                )
            }

            InsightsWeekdayCard(insights = insights, modifier = Modifier.padding(top = 12.dp))

            if (insights.isLongRange) {
                InsightsMonthsCard(
                    months = insights.months,
                    openMonth = openMonth,
                    onMonthClick = { month ->
                        onEvent(BudgetScreenEvents.OnOpenInsightsMonth(month))
                        // The list is below the chart, so bring the chart back into view
                        scope.launch { scrollState.animateScrollTo(chartTop) }
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        } else if (!state.isInsightsLoading) {
            // Not while the first result is loading, or it would flash before the data arrives
            InsightsEmptyCard(category = insights.category, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Preview
@Composable
private fun BudgetInsightsScreenPreview() {
    DenuSpendTheme {
        Surface {
            BudgetInsightsScreenContent(
                state = BudgetScreenState(
                    insightsCategory = "Food",
                    budgetInsights = BudgetInsightsModel(
                        category = "Food",
                        startDate = LocalDate.of(2026, 11, 1),
                        endDate = LocalDate.of(2026, 11, 20),
                        dayCount = 20,
                        spent = 542000L,
                        allowance = 600000L,
                        transactionCount = 38L,
                    )
                ),
                onEvent = {}
            )
        }
    }
}
