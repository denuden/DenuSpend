package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog

/** "Close this wallet?" confirm, shared by the editor sheet and the details screen. */
@Composable
fun CloseWalletDialog(
    showDialog: Boolean,
    walletName: String,
    recurringCount: Int = 0, // a card's active subscriptions / installments, which stop when it's closed
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        showDialog = showDialog,
        onDismissRequest = onDismiss,
        onConfirmation = onConfirm,
        dialogTitle = stringResource(R.string.wallet_dialog_close_title),
        dialogText = stringResource(R.string.wallet_dialog_close_text, walletName) +
            if (recurringCount > 0) pluralStringResource(R.plurals.wallet_dialog_close_recurring, recurringCount, recurringCount) else "",
        icon = Icons.Filled.AccountBalanceWallet,
        confirmText = stringResource(R.string.wallet_btn_close_wallet),
        dismissText = stringResource(R.string.lbl_cancel),
        confirmColor = MaterialTheme.colorScheme.error,
    )
}
