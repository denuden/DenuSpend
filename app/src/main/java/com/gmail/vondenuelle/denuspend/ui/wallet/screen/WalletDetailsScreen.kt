package com.gmail.vondenuelle.denuspend.ui.wallet.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.navigation.toNavOptions
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenEvents
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenState
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletViewModel
import com.gmail.vondenuelle.denuspend.ui.wallet.components.CloseWalletDialog
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletCard
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletEditorSheet
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import com.gmail.vondenuelle.denuspend.utils.formatFirebaseDate
import kotlinx.coroutines.launch

@Composable
fun WalletDetailsScreen(
    walletId: String,
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val wallet = state.wallets.find { it.docId == walletId }

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> onPopBackStack()
            is OneTimeEvents.OnNavigate -> onNavigate(event.route, event.behavior.toNavOptions())
            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
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

    // Pushed screen, so both listeners stop on ON_STOP
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.onEvent(WalletScreenEvents.OnGetWallets)
                viewModel.onEvent(WalletScreenEvents.OnGetWalletTransactions(walletId))
            }
            Lifecycle.Event.ON_STOP -> {
                viewModel.stopWalletsListener()
                viewModel.stopTransactionsListener()
            }
            else -> Unit
        }
    }

    // The wallet was closed (here or on another device): leave once, after the list has loaded at least once.
    // Waits while a close started here is saving, so the VM can send its snackbar before this screen (and the VM) goes away.
    var hasLeft by remember { mutableStateOf(false) }
    LaunchedEffect(state.hasLoadedWallets, wallet == null, state.isLoading) {
        if (state.hasLoadedWallets && wallet == null && !state.isLoading && !hasLeft) {
            hasLeft = true
            onPopBackStack()
        }
    }

    LoadingDialog(showDialog = state.isLoading, text = stringResource(R.string.lbl_loading)) { }

    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    WalletEditorSheet(
        draft = state.walletDraft,
        onDraftChange = { viewModel.onEvent(WalletScreenEvents.OnWalletDraftChanged(it)) },
        onSave = { viewModel.onEvent(WalletScreenEvents.OnSaveWalletDraft) },
        onDismiss = { viewModel.onEvent(WalletScreenEvents.OnCloseWalletEditor) },
        onCloseWallet = if (wallet != null && !wallet.isPhysical) {
            { viewModel.onEvent(WalletScreenEvents.OnCloseWallet(wallet)) }
        } else null,
    )

    var showCloseDialog by rememberSaveable { mutableStateOf(false) }
    CloseWalletDialog(
        showDialog = showCloseDialog && wallet != null,
        walletName = wallet?.name.orEmpty(),
        onDismiss = { showCloseDialog = false },
        onConfirm = {
            showCloseDialog = false
            wallet?.let { viewModel.onEvent(WalletScreenEvents.OnCloseWallet(it)) }
        },
    )

    WalletDetailsScreenContent(
        wallet = wallet,
        state = state,
        onEvent = viewModel::onEvent,
        onCloseWalletClick = { showCloseDialog = true },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun WalletDetailsScreenContent(
    modifier: Modifier = Modifier,
    wallet: WalletModel?,
    state: WalletScreenState,
    onEvent: (WalletScreenEvents) -> Unit,
    onCloseWalletClick: () -> Unit,
) {
    if (wallet == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            WalletCard(wallet = wallet)
            // The physical wallet can't be closed, so Edit takes the full width there
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                FilledTonalButton(
                    onClick = { onEvent(WalletScreenEvents.OnOpenWalletEditor(wallet)) },
                    shape = MaterialTheme.shapes.extraLarge,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text(stringResource(R.string.wallet_btn_edit), maxLines = 1, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                }
                if (!wallet.isPhysical) {
                    OutlinedButton(
                        onClick = onCloseWalletClick,
                        shape = MaterialTheme.shapes.extraLarge,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Text(
                            stringResource(R.string.wallet_btn_close_wallet),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1,
                            modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.wallet_lbl_paid_from_this),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (state.isTransactionsLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (state.walletTransactions.isEmpty()) {
            item {
                if (state.hasTransactionsError) {
                    WalletExpensesPlaceholder(
                        icon = Icons.Outlined.ErrorOutline,
                        title = stringResource(R.string.wallet_empty_error_title),
                        text = stringResource(R.string.wallet_lbl_transactions_error),
                        isError = true
                    )
                } else {
                    WalletExpensesPlaceholder(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = stringResource(R.string.wallet_empty_title),
                        text = stringResource(R.string.wallet_empty_text, wallet.name)
                    )
                }
            }
        }

        // Display only: edit/delete still happen from the Add tab or History, which know the transaction's day
        // The list spans many days but TransactionItem only shows the time, so each new day gets a small date label
        itemsIndexed(state.walletTransactions, key = { _, it -> it.docId }) { index, transaction ->
            val day = formatFirebaseDate(transaction.date.toDate())
            if (index == 0 || formatFirebaseDate(state.walletTransactions[index - 1].date.toDate()) != day) {
                Text(
                    day,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
            TransactionItem(transactionModel = transaction)
        }

        if (state.hasMoreTransactions || state.isLoadingMoreTransactions) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    if (state.isLoadingMoreTransactions) {
                        CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    } else {
                        TextButton(onClick = { onEvent(WalletScreenEvents.OnLoadMoreWalletTransactions(wallet.docId)) }) {
                            Text(stringResource(R.string.wallet_btn_load_more))
                        }
                    }
                }
            }
        }
    }
}

/** Empty or failed list: a soft panel with an icon badge, so the area under the header doesn't look blank. */
@Composable
private fun WalletExpensesPlaceholder(
    icon: ImageVector,
    title: String,
    text: String,
    isError: Boolean = false,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    )
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Preview
@Composable
private fun WalletDetailsScreenEmptyPreview() {
    DenuSpendTheme {
        Surface {
            WalletDetailsScreenContent(
                wallet = WalletModel(docId = "a", name = "GCash", colorIndex = 1, iconKey = "ewallet", balance = 0),
                state = WalletScreenState(),
                onEvent = {},
                onCloseWalletClick = {}
            )
        }
    }
}

@Preview
@Composable
private fun WalletDetailsScreenPreview() {
    DenuSpendTheme {
        Surface {
            WalletDetailsScreenContent(
                wallet = WalletModel(docId = "a", name = "GCash", colorIndex = 1, iconKey = "ewallet", balance = 120000),
                state = WalletScreenState(
                    walletTransactions = listOf(TransactionModel(docId = "1", title = "Lunch", category = "Food", amount = -15000))
                ),
                onEvent = {},
                onCloseWalletClick = {}
            )
        }
    }
}
