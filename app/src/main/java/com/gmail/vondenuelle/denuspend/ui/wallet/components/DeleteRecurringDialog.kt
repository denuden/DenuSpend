package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.RecurringChargeModel
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog

/** "Delete Phone?" for a subscription or installment. Only the schedule goes; what it already charged stays on the card. */
@Composable
fun DeleteRecurringDialog(
    charge: RecurringChargeModel?, // null = hidden
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (charge == null) return
    CustomAlertDialog(
        showDialog = true,
        onDismissRequest = onDismiss,
        onConfirmation = onConfirm,
        dialogTitle = stringResource(R.string.wallet_title_delete_recurring, charge.name),
        dialogText = stringResource(R.string.wallet_text_delete_recurring),
        icon = Icons.Filled.DeleteOutline,
        confirmText = stringResource(R.string.wallet_btn_delete),
        dismissText = stringResource(R.string.lbl_cancel),
        confirmColor = MaterialTheme.colorScheme.error,
    )
}
