package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.imePadding
import com.gmail.vondenuelle.denuspend.ui.add.components.AmountInputSection
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryGrid
import com.gmail.vondenuelle.denuspend.ui.add.components.FormSaveBar
import com.gmail.vondenuelle.denuspend.ui.add.components.FormSectionHeader
import com.gmail.vondenuelle.denuspend.ui.add.components.TitleNoteFields
import com.gmail.vondenuelle.denuspend.ui.add.components.WalletPickerRow
import com.gmail.vondenuelle.denuspend.ui.wallet.components.formatBalance
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils
import kotlin.math.abs
import androidx.compose.material3.Text
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AddExpenseScreen(
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

    // Pushed screen, so the wallet list and today's-doc listeners stop when it's left
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(AddScreenEvents.OnGetWallets)
                viewModel.onEvent(AddScreenEvents.OnGetTodayTotals)
            }
            Lifecycle.Event.ON_STOP -> {
                viewModel.stopWalletsListener()
                viewModel.stopTodayListener()
            }
            else -> Unit
        }
    }

    LaunchedEffect(docId) {
        if (docId != null && dailyDocId != null) {
            viewModel.onEvent(AddScreenEvents.OnLoadTransactionForEdit(docId, dailyDocId))
        }
    }

    AddExpenseScreenContent(
        state = state,
        isEditMode = docId != null,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun AddExpenseScreenContent(
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
                prefixText = "-₱",
                amountColor = MaterialTheme.colorScheme.error,
                tooltipTitle = stringResource(R.string.add_tooltip_expense_title),
                tooltipText = stringResource(R.string.add_tooltip_expense_text),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
                FormSectionHeader(title = stringResource(R.string.add_lbl_expense_category))
                CategoryGrid(selected = state.transactionCategory) {
                    onEvent(AddScreenEvents.OnCategoryChanged(it))
                }
                leftTodayText(state)?.let {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FormSectionHeader(
                    title = stringResource(R.string.wallet_lbl_paid_from),
                    trailingText = walletAfterText(state),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                WalletPickerRow(
                    wallets = state.wallets,
                    hasLoadedWallets = state.hasLoadedWallets,
                    selectedId = state.transactionWalletId,
                    onSelected = { onEvent(AddScreenEvents.OnWalletChanged(it)) },
                )
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
                if (isEditMode) R.string.add_btn_update_expense else R.string.add_btn_save_expense,
                "₱" + CurrencyUtils.formatCents(state.transactionAmount)
            ),
            // Edit mode waits for the original to load, so it can't be saved as a new one by mistake.
            // Both modes wait for the wallet list, so an expense is never saved as "Don't deduct" just because it hadn't arrived yet.
            enabled = state.hasLoadedWallets && (!isEditMode || state.editingTransaction != null),
            onClick = {
                if (isEditMode) {
                    onEvent(AddScreenEvents.OnUpdateTransaction)
                    return@FormSaveBar
                }
                onEvent(AddScreenEvents.OnAddTransaction(
                    TransactionRequest(
                        title = state.transactionTitle,
                        description = state.transactionDescription,
                        amount = -state.transactionAmount, //Make it negative
                        category = state.transactionCategory,
                        walletId = state.transactionWalletId
                    )
                ))
            },
        )
    }
}

// "Food left today: ₱400.00 → ₱250.00" (or "Food overspent today: …") from today's daily doc (allowance minus spent for that category).
// Editing today's expense in the same category adds its original amount back first; editing another day's hides it.
@Composable
private fun leftTodayText(state: AddScreenState): String? {
    val category = state.transactionCategory.ifEmpty { return null }
    val editing = state.editingTransaction
    val isEditingToday = editing != null &&
        editing.date.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    if (editing != null && !isEditingToday) return null

    val daily = state.dailyHistory
    val original = if (editing != null && editing.category == category) abs(editing.amount) else 0L
    val left = (daily.allowanceByCategory[category] ?: 0L) - (daily.expenseByCategory[category] ?: 0L) + original
    val after = left - state.transactionAmount
    // Below zero reads as "overspent" with positive amounts, not "-₱40.00 left"
    return when {
        state.transactionAmount == 0L && left < 0 -> stringResource(R.string.add_lbl_overspent_today, category, formatBalance(-left))
        state.transactionAmount == 0L -> stringResource(R.string.add_lbl_left_today, category, formatBalance(left))
        left < 0 -> stringResource(R.string.add_lbl_overspent_today_after, category, formatBalance(-left), formatBalance(-after))
        after < 0 -> stringResource(R.string.add_lbl_left_today_then_overspent, category, formatBalance(left), formatBalance(-after))
        else -> stringResource(R.string.add_lbl_left_today_after, category, formatBalance(left), formatBalance(after))
    }
}

// "₱500.00 → ₱350.00 after" for the picked wallet. When editing, the original amount was already taken from that wallet,
// so only the difference moves (same math as the service). Null hides it (no wallet, no amount, or the list isn't loaded).
@Composable
private fun walletAfterText(state: AddScreenState): String? {
    val wallet = state.wallets.find { it.docId == state.transactionWalletId } ?: return null
    if (state.transactionAmount == 0L) return null
    val original = state.editingTransaction?.takeIf { it.walletId == wallet.docId }?.amount?.let { abs(it) } ?: 0L
    val after = wallet.balance + original - state.transactionAmount
    return stringResource(R.string.add_lbl_wallet_after, formatBalance(wallet.balance), formatBalance(after))
}

@Preview
@Composable
private fun AddExpenseScreenPreview() {
    DenuSpendTheme() {
        Surface {
            AddExpenseScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = AddScreenState(),
                onEvent = {})
        }
    }
}
