package com.gmail.vondenuelle.denuspend.ui.wallet.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenEvents
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletScreenState
import com.gmail.vondenuelle.denuspend.ui.wallet.WalletViewModel
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletCard
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletEditorFields
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletEditorSheet
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents

/**
 * First-time setup, its own world in AppRoot (between Auth and Main). Step 1 saves the mandatory physical wallet,
 * step 2 lets the user add more. Finish tells MainViewModel, which switches to Main.
 */
@Composable
fun WalletSetupScreen(
    onSetupCompleted: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            // No snackbar host out here (it lives in MainScreen), so "saved" snackbars are skipped
            is OneTimeEvents.ShowSnackbar -> {}
            is OneTimeEvents.OnPopBackStack -> {}
            is OneTimeEvents.OnNavigate -> {}
            OneTimeEvents.OnCloseDialog -> {}
            OneTimeEvents.OnLoginOrRegisterCompleted -> {}
            is OneTimeEvents.ShowInputError -> {}
        }
    }

    // Back on step 2 goes to step 1; on step 1 it leaves the app as usual
    BackHandler(enabled = state.setupStep == 1) {
        viewModel.onEvent(WalletScreenEvents.OnSetupStepBack)
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
    )

    Scaffold { padding ->
        WalletSetupScreenContent(
            state = state,
            onEvent = viewModel::onEvent,
            onFinish = onSetupCompleted,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
fun WalletSetupScreenContent(
    modifier: Modifier = Modifier,
    state: WalletScreenState,
    onEvent: (WalletScreenEvents) -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.wallet_setup_step, state.setupStep + 1, 2),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onEvent(WalletScreenEvents.OnSignOut) }) {
                Text(stringResource(R.string.wallet_setup_btn_sign_out))
            }
        }
        LinearProgressIndicator(
            progress = { (state.setupStep + 1) / 2f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (state.setupStep == 0) {
            Text(
                stringResource(R.string.wallet_setup_title_physical),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.wallet_setup_text_physical),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            WalletEditorFields(
                draft = state.physicalDraft,
                onDraftChange = { onEvent(WalletScreenEvents.OnPhysicalDraftChanged(it)) }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { onEvent(WalletScreenEvents.OnSavePhysicalWallet) },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wallet_setup_btn_next))
            }
        } else {
            Text(
                stringResource(R.string.wallet_setup_title_more),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.wallet_setup_text_more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            state.wallets.forEach { wallet ->
                WalletCard(
                    wallet = wallet,
                    onClick = { onEvent(WalletScreenEvents.OnOpenWalletEditor(wallet)) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            OutlinedButton(
                onClick = { onEvent(WalletScreenEvents.OnOpenWalletEditor(null)) },
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.wallet_btn_add_wallet), modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onFinish,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wallet_setup_btn_finish))
            }
            TextButton(
                onClick = { onEvent(WalletScreenEvents.OnSetupStepBack) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wallet_setup_btn_back))
            }
        }
    }
}

@Preview
@Composable
private fun WalletSetupScreenPreview() {
    DenuSpendTheme {
        Surface {
            WalletSetupScreenContent(
                state = WalletScreenState(physicalDraft = WalletModel(name = "Physical Wallet", iconKey = "wallet")),
                onEvent = {},
                onFinish = {}
            )
        }
    }
}
