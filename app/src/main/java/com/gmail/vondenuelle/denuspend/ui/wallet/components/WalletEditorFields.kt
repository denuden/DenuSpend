package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import kotlin.math.abs

/**
 * Name, balance, color and icon of a wallet. Used inline on the setup screen and inside the add/edit sheet.
 * The balance field only takes positive numbers, so the "negative" switch holds the sign.
 * A credit card swaps the balance for its own fields (CreditCardFields), where the amount owed is stored as a negative balance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletEditorFields(
    modifier: Modifier = Modifier,
    draft: WalletModel,
    onDraftChange: (WalletModel) -> Unit,
    // Only when adding (and never on setup's physical step): the type is locked once saved, since switching a wallet with history
    // to or from a card would rewrite its balance and statements
    showCreditCardOption: Boolean = draft.docId.isEmpty() && !draft.isPhysical,
) {
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    // Keyed on the doc so opening another wallet resets it. A ₱0 balance has no sign, so the switch keeps it here.
    var isNegative by rememberSaveable(draft.docId) { mutableStateOf(draft.balance < 0) }
    // What the new wallet was before the credit card switch was turned on, so turning it off puts it back
    var negativeBeforeCard by rememberSaveable(draft.docId) { mutableStateOf<Boolean?>(null) }
    var iconBeforeCard by rememberSaveable(draft.docId) { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        // Live preview of the card
        WalletCard(wallet = draft)

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_name))
        OutlinedTextField(
            value = draft.name,
            onValueChange = { if (it.length <= 20) onDraftChange(draft.copy(name = it)) },
            placeholder = { Text(stringResource(R.string.wallet_hint_name)) },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = outlinedColor,
                disabledIndicatorColor = outlinedColor,
                errorIndicatorColor = outlinedColor
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth()
        )

        if (showCreditCardOption) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Icon(
                    Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.wallet_lbl_credit_card_switch), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.wallet_lbl_credit_card_switch_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Turning it on keeps the typed amount as what's owed. Turning it off puts back the sign and icon from before it was
                // turned on (so an accidental on/off leaves ₱20,000 as ₱20,000).
                Switch(
                    checked = draft.isCreditCard,
                    onCheckedChange = { on ->
                        if (on) {
                            negativeBeforeCard = isNegative
                            iconBeforeCard = draft.iconKey
                            onDraftChange(
                                draft.copy(
                                    type = WalletModel.TYPE_CREDIT_CARD,
                                    balance = -abs(draft.balance),
                                    iconKey = if (draft.iconKey.isEmpty() || draft.iconKey == "wallet") "card" else draft.iconKey
                                )
                            )
                        } else {
                            val restoreNegative = negativeBeforeCard ?: (draft.balance < 0)
                            isNegative = restoreNegative
                            onDraftChange(
                                draft.copy(
                                    type = "",
                                    balance = if (restoreNegative) -abs(draft.balance) else abs(draft.balance),
                                    // Only undo the automatic "card" icon, not one the user picked while it was on
                                    iconKey = iconBeforeCard?.takeIf { draft.iconKey == "card" } ?: draft.iconKey
                                )
                            )
                        }
                    }
                )
            }
        }

        if (draft.isCreditCard) {
            CreditCardFields(draft = draft, onDraftChange = onDraftChange)
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            FieldLabel(stringResource(R.string.wallet_lbl_current_balance))
            CurrencyTextField(
                amountInCents = abs(draft.balance),
                onAmountChange = { onDraftChange(draft.copy(balance = if (isNegative) -it else it)) },
                prefixText = if (isNegative) "-₱" else "₱",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.wallet_lbl_negative_switch),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isNegative,
                    onCheckedChange = {
                        isNegative = it
                        onDraftChange(draft.copy(balance = if (it) -abs(draft.balance) else abs(draft.balance)))
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_color))
        FlowRow(
            horizontalArrangement = Arrangement.SpaceBetween, // spread across the full width, like the icon grid below
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 7, // 14 colors = 2 even rows
            modifier = Modifier.fillMaxWidth()
        ) {
            WalletStyle.colors.forEachIndexed { index, color ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onDraftChange(draft.copy(colorIndex = index)) }
                ) {
                    if (draft.colorIndex == index) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_icon))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WalletStyle.icons.forEach { (key, icon) ->
                val selected = draft.iconKey == key || (draft.iconKey.isEmpty() && key == "wallet")
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .border(
                            width = if (selected) 2.dp else 0.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onDraftChange(draft.copy(iconKey = key)) }
                ) {
                    Icon(
                        icon,
                        contentDescription = key,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
