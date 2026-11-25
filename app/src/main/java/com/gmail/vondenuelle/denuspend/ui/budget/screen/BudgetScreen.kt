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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetChartFilter
import com.gmail.vondenuelle.denuspend.domain.models.budget.BudgetTotalSummaryModel
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenEvents
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenState
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetViewModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetBillsCard
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetItem
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.ChartSection
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch
import com.gmail.vondenuelle.denuspend.ui.budget.components.formatPickerRange
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetRangePickerDialog

@Composable
fun BudgetScreen(
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


    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(BudgetScreenEvents.OnGetBudgetSummary) //initial
            }
            // No ON_STOP stop: this is a bottom-nav tab screen, so the listener keeps running and the data stays cached
            else -> Unit
        }
    }

    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    if (state.showRangePicker) {
        BudgetRangePickerDialog(
            onDismiss = { viewModel.onEvent(BudgetScreenEvents.OnShowRangePicker(false)) },
            onConfirm = { start, end -> viewModel.onEvent(BudgetScreenEvents.OnCustomRangeSelected(start, end)) }
        )
    }

    BudgetScreenContent(
        state = state, onEvent = viewModel::onEvent,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreenContent(
    modifier: Modifier = Modifier,
    state: BudgetScreenState,
    onEvent: (BudgetScreenEvents) -> Unit,
    ) {
    val scrollState = rememberScrollState()
    val summary = state.budgetTotalSummaryModel

    // Biggest spending first. Categories with no spending and no allowance are only named in one line under the list.
    val (active, unused) = CategoryDropdownButtonModel.list.drop(1)
        .mapIndexed { index, name -> index to name }
        .partition { (_, name) -> (summary.totals[name] ?: 0L) > 0 || (summary.allowances[name] ?: 0L) > 0 }
    val activeSorted = active.sortedWith(
        compareByDescending<Pair<Int, String>> { (_, name) -> summary.totals[name] ?: 0L }
            .thenByDescending { (_, name) -> summary.allowances[name] ?: 0L }
    )

    val filterLabel = if (state.filter == BudgetChartFilter.CUSTOM && state.customStartMillis != null) {
        formatPickerRange(state.customStartMillis, state.customEndMillis ?: state.customStartMillis)
    } else {
        stringResource(state.filter.label)
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Thin loading bar instead of a dialog. The slot keeps its height so nothing jumps.
        Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.Center) {
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        ChartSection(
            filterLabel = filterLabel,
            budgetData = summary,
            onChangeFilter = {
                onEvent(BudgetScreenEvents.OnChangeFilter(it))
            }
        )

        if (activeSorted.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.budget_lbl_breakdown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            //list of categories
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    activeSorted.forEach { (index, name) ->
                        BudgetItem(
                            category = name,
                            icon = CategoryDropdownButtonModel.icons[index],
                            color = CategoryDropdownButtonModel.colors[index],
                            iconTint = CategoryDropdownButtonModel.selectedColors[index],
                            amount = summary.totals[name] ?: 0L,
                            allowance = summary.allowances[name] ?: 0L,
                            transactionCount = summary.counts[name] ?: 0,
                            onClick = {
                                onEvent(BudgetScreenEvents.OnNavigateToBudgetTransactionScreen(name))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (unused.isNotEmpty()) {
                Text(
                    stringResource(R.string.budget_lbl_no_spending, unused.joinToString(", ") { it.second }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
            }
        }

        // Only for someone with a card (or charges from a card that was closed since)
        val bills = state.budgetBills
        if (bills.hasCards || bills.rows.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            BudgetBillsCard(
                bills = bills,
                emptyText = stringResource(
                    if (state.filter == BudgetChartFilter.THIS_MONTH) R.string.budget_lbl_bills_empty_month else R.string.budget_lbl_bills_empty
                ),
                onOpenCard = { onEvent(BudgetScreenEvents.OnOpenBillCard(it)) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
private fun BudgetScreenContentPreview() {
    DenuSpendTheme {
        Surface {
            BudgetScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = BudgetScreenState(
                    budgetTotalSummaryModel = BudgetTotalSummaryModel(
                        totals = mapOf("Food" to 499500L, "Transportation" to 310000L, "Personal" to 120000L),
                        counts = mapOf("Food" to 12, "Transportation" to 8, "Personal" to 1),
                        totalExpense = 929500L,
                        allowances = mapOf("Food" to 600000L, "Transportation" to 300000L, "Household" to 200000L),
                        totalAllowance = 1100000L
                    )
                ),
                onEvent =  {}
            )
        }
    }
}
