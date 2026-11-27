package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.wallet.components.formatBalance
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import kotlin.math.abs

/**
 * Bottom sheet content for a tapped transaction: Edit, or Delete (behind a confirm dialog).
 */
@Composable
fun TransactionActionsSheet(
    modifier: Modifier = Modifier,
    transaction: TransactionModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    CustomAlertDialog(
        showDialog = showDeleteConfirm,
        onDismissRequest = { showDeleteConfirm = false },
        onConfirmation = {
            showDeleteConfirm = false
            onDelete()
        },
        dialogTitle = stringResource(R.string.add_dialog_delete_title),
        // An expense paid from a wallet also gives the money back there, so the dialog says so
        dialogText = if (transaction.walletId.isNotBlank()) stringResource(R.string.add_dialog_delete_text_wallet, transaction.title, formatBalance(abs(transaction.amount)))
        else stringResource(R.string.add_dialog_delete_text, transaction.title),
        icon = Icons.Default.Delete,
        confirmText = stringResource(R.string.add_btn_delete),
        dismissText = stringResource(R.string.lbl_cancel),
        confirmColor = MaterialTheme.colorScheme.error,
    )

    Column(modifier = modifier.padding(16.dp)) {
        TransactionItem(transactionModel = transaction)

        Text(
            stringResource(R.string.add_lbl_category, transaction.category),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                shape = MaterialTheme.shapes.extraLarge,
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.add_btn_delete))
            }

            Spacer(modifier = Modifier.width(12.dp))

            FilledTonalButton(
                shape = MaterialTheme.shapes.extraLarge,
                onClick = onEdit,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.add_btn_edit))
            }
        }
    }
}

@Preview
@Composable
private fun TransactionActionsSheetPreview() {
    DenuSpendTheme {
        Surface {
            TransactionActionsSheet(
                modifier = Modifier.fillMaxWidth(),
                transaction = TransactionModel(title = "Groceries", description = "Weekly", amount = -150000, category = "Food"),
                onEdit = {},
                onDelete = {}
            )
        }
    }
}
