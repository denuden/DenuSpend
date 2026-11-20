package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.AllowanceTemplateModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionActionsSheet
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionListControls
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionSort
import com.gmail.vondenuelle.denuspend.ui.add.components.TransactionTypeFilter
import com.gmail.vondenuelle.denuspend.ui.add.components.arrangeTransactions
import com.gmail.vondenuelle.denuspend.ui.add.components.AddAllowanceSheetContent
import com.gmail.vondenuelle.denuspend.ui.add.components.transactionItems
import com.gmail.vondenuelle.denuspend.ui.add.components.section.AddButtonsSection
import com.gmail.vondenuelle.denuspend.ui.add.components.section.TodayIncomeAndExpensesSection
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.common.components.PullToRefreshCustomStyle
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

    // The transaction the actions sheet is showing; null means the sheet is closed.
    var selectedTransaction by remember { mutableStateOf<TransactionModel?>(null) }
    // The template waiting for "add for today" confirmation; null means the dialog is closed.
    var templateToApply by remember { mutableStateOf<AllowanceTemplateModel?>(null) }
    // The "Add Allowance" sheet (custom form or a template). Saveable, so it stays open on rotation.
    var showAllowanceSheet by rememberSaveable { mutableStateOf(false) }

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

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

    // Bottom-nav tab screen: listeners start once and keep running (no ON_STOP stop), so coming back shows the
    // cached data right away instead of re-reading Firestore. ON_START only restarts a listener that ended (e.g. after an error).
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(AddScreenEvents.OnGetTransactionOverview)
                viewModel.onEvent(AddScreenEvents.OnGetAllowanceTemplates)
            }
            else -> Unit
        }
    }

    LoadingDialog(
        showDialog = state.isLoading,
        text = stringResource(R.string.lbl_loading),
    ) { }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    templateToApply?.let { template ->
        CustomAlertDialog(
            showDialog = true,
            onDismissRequest = { templateToApply = null },
            onConfirmation = {
                templateToApply = null
                showAllowanceSheet = false
                viewModel.onEvent(AddScreenEvents.OnApplyAllowanceTemplate(template))
            },
            dialogTitle = stringResource(R.string.add_dialog_apply_template_title, template.name),
            dialogText = pluralStringResource(
                R.plurals.add_dialog_apply_template_text,
                template.items.size,
                template.items.size,
                "₱${CurrencyUtils.formatCents(template.items.sumOf { it.amount })}"
            ),
            icon = Icons.Filled.Add,
            confirmText = stringResource(R.string.add_btn_add),
            dismissText = stringResource(R.string.lbl_cancel),
        )
    }

    ModalBottomSheetDialog(
        showDialog = showAllowanceSheet,
        onDismissRequest = { showAllowanceSheet = false },
    ) {
        AddAllowanceSheetContent(
            modifier = Modifier.fillMaxWidth(),
            templates = state.allowanceTemplates,
            hasLoaded = state.hasLoadedTemplates,
            onCustomClick = {
                showAllowanceSheet = false
                viewModel.onEvent(AddScreenEvents.OnNavigateToIncomeScreen)
            },
            // The confirm dialog opens on top of the sheet; confirming closes both
            onTemplateClick = { templateToApply = it },
            onManageClick = {
                showAllowanceSheet = false
                viewModel.onEvent(AddScreenEvents.OnNavigateToAllowanceTemplates)
            },
        )
    }

    ModalBottomSheetDialog(
        showDialog = selectedTransaction != null,
        onDismissRequest = { selectedTransaction = null },
    ) {
        selectedTransaction?.let { selected ->
            // Today's list only, so the day doc is today's overview doc
            val dailyDocId = state.dailyHistory.docId
            TransactionActionsSheet(
                modifier = Modifier.fillMaxWidth(),
                transaction = selected,
                onEdit = {
                    selectedTransaction = null
                    viewModel.onEvent(AddScreenEvents.OnNavigateToEditTransaction(selected, dailyDocId))
                },
                onDelete = {
                    selectedTransaction = null
                    viewModel.onEvent(AddScreenEvents.OnDeleteTransaction(selected, dailyDocId))
                }
            )
        }
    }

    AddScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onTransactionClick = { selectedTransaction = it },
        onAddAllowanceClick = { showAllowanceSheet = true }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddScreenContent(
    modifier: Modifier = Modifier,
    state: AddScreenState,
    onEvent: (AddScreenEvents) -> Unit,
    onTransactionClick: (TransactionModel) -> Unit = {},
    onAddAllowanceClick: () -> Unit = {},
) {
    // Display-only choices for the "Last Added" list, so they stay in the screen (rememberSaveable keeps them on rotation)
    var typeFilter by rememberSaveable { mutableStateOf(TransactionTypeFilter.ALL) }
    var sort by rememberSaveable { mutableStateOf(TransactionSort.LATEST) }
    var groupByCategory by rememberSaveable { mutableStateOf(false) }
    val shownTransactions = remember(state.transactionList, typeFilter, sort) {
        arrangeTransactions(state.transactionList, typeFilter, sort)
    }

    val expense = state.dailyHistory.totalExpense
    val income = state.dailyHistory.totalIncome

    val progress: Float = when {
        income > 0 -> expense.toFloat() / income.toFloat()
        expense > 0 -> 1f // spent with no income yet, show as full
        else -> 0f // avoid division by zero
    }

    // Animate the progress value
    // Kept outside the LazyColumn, so scrolling the card away and back doesn't replay the animation
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f), // keep within [0,1]
        animationSpec = tween(
            durationMillis = 800, // adjust speed
            easing = FastOutSlowInEasing // smooth curve
        )
    )

    // Pull to refresh only does the midnight / dead-listener check (the data is already live), see OnRefreshOverview
    PullToRefreshCustomStyle(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(AddScreenEvents.OnRefreshOverview) },
        modifier = modifier.fillMaxSize()
    ) {
        // One list for the whole screen, so the card, buttons and templates scroll away and the list gets the room.
        // The filter chips stick to the top while scrolling.
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier
                .fillMaxSize() //full height, so a short list can still be pulled
                .padding(horizontal = 16.dp)
        ) {
            item {
                TodayIncomeAndExpensesSection(
                    modifier = Modifier.fillMaxWidth(),
                    progress = animatedProgress,
                    expense =   "₱${CurrencyUtils.formatCents(expense)}",
                    totalBudget = "₱${CurrencyUtils.formatCents(income)}",
                    remaining = if (expense > income) stringResource(R.string.add_lbl_amount_over, "₱${CurrencyUtils.formatCents(expense - income)}")
                        else stringResource(R.string.add_lbl_amount_left, "₱${CurrencyUtils.formatCents(income - expense)}"),
                    isOverspent = expense > income,
                )
                Spacer(modifier = Modifier.height(8.dp))

                AddButtonsSection(modifier = Modifier.fillMaxWidth(), onExpenseClick = {
                    onEvent(AddScreenEvents.OnNavigateToExpenseScreen)

                },  onIncomeClick= {
                    // Opens the sheet (custom allowance or a template) instead of going straight to the form
                    onAddAllowanceClick()
                })

                // Space goes above the section, so "Last Added" reads as its own section
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.add_lbl_last_added),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Plain text instead of a TextButton: the button's 40dp min height made a big gap above the chips
                    Text(
                        stringResource(R.string.main_title_history),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onEvent(AddScreenEvents.OnNavigateToRecentTransactions) }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }

            stickyHeader {
                // Solid background so the rows scroll underneath instead of showing through
                TransactionListControls(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 4.dp),
                    type = typeFilter,
                    onTypeChange = { typeFilter = it },
                    sort = sort,
                    onSortChange = { sort = it },
                    groupByCategory = groupByCategory,
                    onGroupByCategoryChange = { groupByCategory = it }
                )
            }

            transactionItems(shownTransactions, groupByCategory, onTransactionClick)
            if(shownTransactions.isEmpty()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(stringResource(R.string.add_lbl_nothing_to_show), fontWeight = FontWeight.W300)
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