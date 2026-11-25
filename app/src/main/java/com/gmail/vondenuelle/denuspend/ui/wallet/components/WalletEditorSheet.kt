package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog

/**
 * Add / edit sheet. draft != null = open (empty docId = new wallet).
 * onCloseWallet = null hides "Close wallet" (new wallets and the physical wallet).
 */
@Composable
fun WalletEditorSheet(
    draft: WalletModel?,
    onDraftChange: (WalletModel) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onCloseWallet: (() -> Unit)? = null,
    recurringCount: Int = 0, // passed to the close confirm
) {
    var showCloseDialog by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheetDialog(
        showDialog = draft != null,
        onDismissRequest = onDismiss,
    ) {
        if (draft == null) return@ModalBottomSheetDialog
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                stringResource(if (draft.docId.isEmpty()) R.string.wallet_title_add else R.string.wallet_title_edit),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            WalletEditorFields(draft = draft, onDraftChange = onDraftChange)

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSave,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.wallet_btn_save))
            }
            if (onCloseWallet != null) {
                TextButton(
                    onClick = { showCloseDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.wallet_btn_close_wallet), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    CloseWalletDialog(
        showDialog = showCloseDialog && draft != null,
        walletName = draft?.name.orEmpty(),
        recurringCount = recurringCount,
        onDismiss = { showCloseDialog = false },
        onConfirm = {
            showCloseDialog = false
            onCloseWallet?.invoke()
        },
    )
}
