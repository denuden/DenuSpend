package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog
import kotlin.math.abs

/** "Delete this entry?" for a money in/out entry. Says where the money goes back, since deleting moves the balance. */
@Composable
fun DeleteMovementDialog(
    movement: WalletMovementModel?, // null = hidden
    walletName: String,
    otherWalletName: String?, // transfers only (null = closed)
    isCreditCard: Boolean = false, // this wallet is a card
    isOtherCreditCard: Boolean = false, // the transfer's other side is a card (a card payment seen from the bank)
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (movement == null) return
    val amount = formatBalance(abs(movement.amount))
    // A transfer whose other wallet was closed only moves this wallet, so it reads like a plain in/out entry
    val isOpenTransfer = movement.isTransfer && otherWalletName != null
    val text = when {
        // On a card the balance is what's owed, so "put back into Visa" would read backwards
        isOpenTransfer && isCreditCard -> stringResource(R.string.wallet_dialog_delete_card_payment, amount, otherWalletName.orEmpty(), walletName)
        isOpenTransfer && isOtherCreditCard -> stringResource(R.string.wallet_dialog_delete_card_payment, amount, walletName, otherWalletName.orEmpty())
        isCreditCard && movement.amount > 0 -> stringResource(R.string.wallet_dialog_delete_card_credit, amount, walletName)
        isCreditCard -> stringResource(R.string.wallet_dialog_delete_card_charge, amount, walletName)
        // Money goes back to the wallet it came out of
        isOpenTransfer && movement.amount < 0 -> stringResource(R.string.wallet_dialog_delete_movement_transfer, amount, otherWalletName.orEmpty(), walletName)
        isOpenTransfer -> stringResource(R.string.wallet_dialog_delete_movement_transfer, amount, walletName, otherWalletName.orEmpty())
        movement.amount > 0 -> stringResource(R.string.wallet_dialog_delete_movement_in, amount, walletName)
        else -> stringResource(R.string.wallet_dialog_delete_movement_out, amount, walletName)
    }

    CustomAlertDialog(
        showDialog = true,
        onDismissRequest = onDismiss,
        onConfirmation = onConfirm,
        dialogTitle = stringResource(R.string.wallet_dialog_delete_movement_title),
        dialogText = text,
        icon = Icons.Filled.DeleteOutline,
        confirmText = stringResource(R.string.wallet_btn_delete),
        dismissText = stringResource(R.string.lbl_cancel),
        confirmColor = MaterialTheme.colorScheme.error,
    )
}
