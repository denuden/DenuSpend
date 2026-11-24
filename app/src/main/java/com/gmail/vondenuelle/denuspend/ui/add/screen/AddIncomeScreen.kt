package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request.TransactionRequest
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.ui.add.components.AmountInputSection
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryGrid
import com.gmail.vondenuelle.denuspend.ui.add.components.FormSaveBar
import com.gmail.vondenuelle.denuspend.ui.add.components.FormSectionHeader
import com.gmail.vondenuelle.denuspend.ui.add.components.TitleNoteFields
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.components.formatBalance
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

@Composable
fun AddIncomeScreen(
    docId: String? = null, // set = edit mode
    dailyDocId: String? = null,
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
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

    LoadingDialog(
        showDialog = state.isLoading,
        text = stringResource(R.string.lbl_loading),
    ) { }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    // Pushed screen, so today's-doc listener stops when it's left (shares the Add tab's listener, ~0 reads)
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onEvent(AddScreenEvents.OnGetTodayTotals)
            Lifecycle.Event.ON_STOP -> viewModel.stopTodayListener()
            else -> Unit
        }
    }

    LaunchedEffect(docId) {
        if (docId != null && dailyDocId != null) {
            viewModel.onEvent(AddScreenEvents.OnLoadTransactionForEdit(docId, dailyDocId))
        }
    }

    AddIncomeScreenContent(
        state = state,
        isEditMode = docId != null,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun AddIncomeScreenContent(
    modifier: Modifier = Modifier,
    state: AddScreenState,
    isEditMode: Boolean = false,
    onEvent: (AddScreenEvents) -> Unit
) {
    val scrollState = rememberScrollState()

    // Only the form scrolls; the Save bar stays pinned at the bottom (and rides above the keyboard)
    Column(modifier = modifier.imePadding()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp)
        ) {
            AmountInputSection(
                amountInCents = state.transactionAmount,
                onAmountChange = { onEvent(AddScreenEvents.OnAmountChanged(it)) },
                prefixText = "+₱",
                amountColor = colorResource(R.color.verified_green_text),
                tooltipTitle = stringResource(R.string.add_tooltip_allowance_title),
                tooltipText = stringResource(R.string.add_tooltip_allowance_text),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                // The "today" hint sits above the grid, so it's still in view after scrolling down to pick a category
                FormSectionHeader(title = stringResource(R.string.add_lbl_allowance_for), supportingText = allowanceTodayText(state))
                CategoryGrid(selected = state.transactionCategory) {
                    onEvent(AddScreenEvents.OnCategoryChanged(it))
                }
            }

            TitleNoteFields(
                title = state.transactionTitle,
                note = state.transactionDescription,
                onTitleChange = { onEvent(AddScreenEvents.OnTitleChanged(it)) },
                onNoteChange = { onEvent(AddScreenEvents.OnDescriptionChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        FormSaveBar(
            text = stringResource(
                if (isEditMode) R.string.add_btn_update_allowance else R.string.add_btn_save_allowance,
                "₱" + CurrencyUtils.formatCents(state.transactionAmount)
            ),
            // Edit mode waits for the original to load, so it can't be saved as a new one by mistake
            enabled = !isEditMode || state.editingTransaction != null,
            onClick = {
                if (isEditMode) {
                    onEvent(AddScreenEvents.OnUpdateTransaction)
                    return@FormSaveBar
                }
                onEvent(AddScreenEvents.OnAddTransaction(
                    TransactionRequest(
                        title = state.transactionTitle,
                        description = state.transactionDescription,
                        amount = state.transactionAmount,
                        category = state.transactionCategory
                    )
                ))
            },
        )
    }
}

// "Food today: ₱200.00 → ₱500.00" from today's daily doc (the category's allowance so far, then with this one).
// Editing today's allowance in the same category swaps its original amount for the new one; editing another day's hides it.
@Composable
private fun allowanceTodayText(state: AddScreenState): String? {
    val category = state.transactionCategory.ifEmpty { return null }
    val editing = state.editingTransaction
    val isEditingToday = editing != null &&
        editing.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    if (editing != null && !isEditingToday) return null

    val today = state.dailyHistory.allowanceByCategory[category] ?: 0L
    val original = if (editing != null && editing.category == category) abs(editing.amount) else 0L
    val after = today - original + state.transactionAmount
    return if (state.transactionAmount == 0L) {
        stringResource(R.string.add_lbl_allowance_today, category, formatBalance(today))
    } else {
        stringResource(R.string.add_lbl_allowance_today_after, category, formatBalance(today), formatBalance(after))
    }
}

@Preview
@Composable
private fun AddIncomeScreenPreview() {
    DenuSpendTheme() {
        Surface {
            AddIncomeScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = AddScreenState(),
                onEvent = {})
        }
    }
}
