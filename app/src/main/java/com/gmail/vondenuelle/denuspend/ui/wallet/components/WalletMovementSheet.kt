package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import com.gmail.vondenuelle.denuspend.ui.add.components.WalletPickerRow
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.MovementDraft
import com.gmail.vondenuelle.denuspend.ui.wallet.MovementType

/**
 * Add money / Take out / Transfer for one wallet. draft != null = open.
 * The preset chips only fill the title, so any title can be typed instead (and typing over a preset drops its icon).
 */
@Composable
fun WalletMovementSheet(
    draft: MovementDraft?,
    wallet: WalletModel?,
    wallets: List<WalletModel>,
    hasLoadedWallets: Boolean,
    onDraftChange: (MovementDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheetDialog(
        showDialog = draft != null && wallet != null,
        onDismissRequest = onDismiss,
        skipPartiallyExpanded = true,
    ) {
        if (draft == null || wallet == null) return@ModalBottomSheetDialog
        WalletMovementSheetContent(
            draft = draft,
            wallet = wallet,
            wallets = wallets,
            hasLoadedWallets = hasLoadedWallets,
            onDraftChange = onDraftChange,
            onSave = onSave
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletMovementSheetContent(
    draft: MovementDraft,
    wallet: WalletModel,
    wallets: List<WalletModel>,
    hasLoadedWallets: Boolean,
    onDraftChange: (MovementDraft) -> Unit,
    onSave: () -> Unit,
) {
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val fieldColors = OutlinedTextFieldDefaults.colors().copy(
        unfocusedIndicatorColor = outlinedColor,
        disabledIndicatorColor = outlinedColor,
        errorIndicatorColor = outlinedColor
    )
    val isIn = draft.type == MovementType.IN
    val presets = when (draft.type) {
        MovementType.IN -> WalletMovementPresets.moneyIn
        MovementType.OUT -> WalletMovementPresets.moneyOut
        MovementType.TRANSFER -> emptyList()
    }
    val otherWallets = wallets.filter { it.docId != wallet.docId }
    val canSave = draft.amount > 0 && draft.title.isNotBlank() &&
        (draft.type != MovementType.TRANSFER || otherWallets.any { it.docId == draft.toWalletId })
    val after = if (isIn) wallet.balance + draft.amount else wallet.balance - draft.amount

    // Only the fields scroll; Save stays pinned under them, so it isn't hidden behind the keyboard while typing
    Column(modifier = Modifier.imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(
                        when (draft.type) {
                            MovementType.IN -> R.string.wallet_title_add_money
                            MovementType.OUT -> R.string.wallet_title_take_out
                            MovementType.TRANSFER -> R.string.wallet_title_transfer
                        },
                        wallet.name
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                CurrencyTextField(
                    amountInCents = draft.amount,
                    onAmountChange = { onDraftChange(draft.copy(amount = it)) },
                    prefixText = if (isIn) "+₱" else "-₱",
                    inlinePrefix = true,
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (isIn) colorResource(R.color.verified_green_text) else MaterialTheme.colorScheme.error
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    stringResource(R.string.wallet_lbl_movement_after, formatBalance(wallet.balance), formatBalance(after)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (draft.type == MovementType.TRANSFER) {
                FieldLabel(stringResource(R.string.wallet_lbl_transfer_to), modifier = Modifier.padding(start = 16.dp, top = 16.dp))
                WalletPickerRow(
                    wallets = otherWallets,
                    hasLoadedWallets = hasLoadedWallets,
                    selectedId = draft.toWalletId,
                    onSelected = { onDraftChange(draft.copy(toWalletId = it)) },
                    showNoWalletOption = false,
                )
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                FieldLabel(stringResource(R.string.wallet_lbl_movement_title))
                OutlinedTextField(
                    value = draft.title,
                    // A typed title is the user's own, so it no longer counts as the preset (it gets the generic icon)
                    onValueChange = { if (it.length <= 30) onDraftChange(draft.copy(title = it, presetKey = "")) },
                    placeholder = { Text(stringResource(R.string.wallet_hint_movement_title)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth()
                )

                if (presets.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        presets.forEach { preset ->
                            val label = stringResource(preset.label)
                            FilterChip(
                                selected = draft.presetKey == preset.key,
                                onClick = { onDraftChange(draft.copy(title = label, presetKey = preset.key)) },
                                label = { Text(label) },
                                leadingIcon = { Icon(preset.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                FieldLabel(stringResource(R.string.wallet_lbl_movement_note))
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { if (it.length <= 50) onDraftChange(draft.copy(note = it.replace('\n', ' '))) },
                    placeholder = { Text(stringResource(R.string.wallet_hint_movement_note)) },
                    maxLines = 2,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
        ) {
            Text(stringResource(R.string.wallet_btn_save_movement, formatBalance(draft.amount)))
        }
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = modifier.padding(bottom = 4.dp)
    )
}

@Preview
@Composable
private fun WalletMovementSheetPreview() {
    DenuSpendTheme {
        Surface {
            WalletMovementSheetContent(
                draft = MovementDraft(type = MovementType.OUT, amount = 80000, title = "Emergency", presetKey = "emergency"),
                wallet = WalletModel(docId = "g", name = "GCash", colorIndex = 1, iconKey = "ewallet", balance = 345000),
                wallets = emptyList(),
                hasLoadedWallets = true,
                onDraftChange = {},
                onSave = {}
            )
        }
    }
}
