package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButton
import com.gmail.vondenuelle.denuspend.ui.common.components.InformationTooltip
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch
import com.gmail.vondenuelle.denuspend.ui.add.components.WalletDropdownButton

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
        text = "Loading...",
    ) { }

    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    // Pushed screen, so the wallet list listener stops when it's left
    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> viewModel.onEvent(AddScreenEvents.OnGetWallets)
            Lifecycle.Event.ON_STOP -> viewModel.stopWalletsListener()
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

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        ElevatedCard(
            colors = CardDefaults.cardColors()
                .copy(containerColor = colorResource(R.color.not_verified_red_container)),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                InformationTooltip(
                    richTooltipSubheadText = "Adding Expense Transaction",
                    richTooltipText = "Add the amount of the expense transaction. You can input a title and description (E.G. Subscription for movies, From entertainment savings) ",
                    modifier = Modifier.padding(8.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
                ) {
                    Text("Enter Amount", style = MaterialTheme.typography.titleSmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(contentAlignment = Alignment.Center) {
                        CurrencyTextField(
                            amountInCents = state.transactionAmount,
                            onAmountChange = {
                                onEvent(AddScreenEvents.OnAmountChanged(it))
                            },
                            prefixText = "-₱",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            modifier = Modifier
                                .padding(top = 32.dp)
                                .fillMaxWidth(0.75f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

        Spacer(modifier = Modifier.height(32.dp))


        Text(
            "Title (20 characters)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = state.transactionTitle,
            onValueChange = {
                if (it.length <= 20) {
                    onEvent(AddScreenEvents.OnTitleChanged(it))
                }
            },
            placeholder = { Text("Enter a title for this transaction") },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = outlinedColor,
                disabledIndicatorColor = outlinedColor,
                errorIndicatorColor = outlinedColor
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),

            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Description (50 characters | optional)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = state.transactionDescription,
            onValueChange = {
                if (it.length <= 50) {
                    onEvent(AddScreenEvents.OnDescriptionChanged(it))
                }
            },
            placeholder = { Text("Enter a short description of your transaction") },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = outlinedColor,
                disabledIndicatorColor = outlinedColor,
                errorIndicatorColor = outlinedColor
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer( modifier = Modifier. height(16.dp))
        Text(
            "Expense Category",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        CategoryDropdownButton(modifier = Modifier.fillMaxWidth(), selected = state.transactionCategory){
            onEvent(AddScreenEvents.OnCategoryChanged(it))
        }

        Spacer( modifier = Modifier. height(16.dp))
        Text(
            stringResource(R.string.wallet_lbl_paid_from),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        WalletDropdownButton(
            wallets = state.wallets,
            hasLoadedWallets = state.hasLoadedWallets,
            selectedId = state.transactionWalletId,
            onSelected = { onEvent(AddScreenEvents.OnWalletChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer( modifier = Modifier. height(16.dp))
        FilledTonalButton(
            shape = MaterialTheme.shapes.extraLarge,
            // Edit mode waits for the original to load, so it can't be saved as a new one by mistake
            enabled = !isEditMode || state.editingTransaction != null,
            onClick = {
                if (isEditMode) {
                    onEvent(AddScreenEvents.OnUpdateTransaction)
                    return@FilledTonalButton
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
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEditMode) stringResource(R.string.add_btn_update_transaction) else "Save Transaction")
        }
    }


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
