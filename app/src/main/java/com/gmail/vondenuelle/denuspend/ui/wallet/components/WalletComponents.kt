package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CurrencyTextField
import com.gmail.vondenuelle.denuspend.ui.common.dialog.CustomAlertDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ModalBottomSheetDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.formatCents
import kotlin.math.abs

/**
 * The colors and icons a wallet can pick. Only the index / key is saved in Firestore.
 * Hardcoded on purpose: these are the user's card colors, not theme colors, and white text must stay readable on all of them.
 */
object WalletStyle {
    val colors = listOf(
        Color(0xFF37474F), // slate
        Color(0xFF1565C0), // blue
        Color(0xFF283593), // indigo
        Color(0xFF00897B), // teal
        Color(0xFF2E7D32), // green
        Color(0xFF6A1B9A), // purple
        Color(0xFFAD1457), // pink
        Color(0xFFC62828), // red
        Color(0xFFEF6C00), // orange
        Color(0xFF5D4037), // brown
    )

    // Keys are what's stored, so never rename one (old wallets would fall back to the default icon)
    val icons = linkedMapOf(
        "wallet" to Icons.Filled.AccountBalanceWallet,
        "cash" to Icons.Filled.Payments,
        "bank" to Icons.Filled.AccountBalance,
        "card" to Icons.Filled.CreditCard,
        "ewallet" to Icons.Filled.PhoneAndroid,
        "qr" to Icons.Filled.QrCode2,
        "savings" to Icons.Filled.Savings,
        "money" to Icons.Filled.AttachMoney,
        "lock" to Icons.Filled.Lock,
        "work" to Icons.Filled.Work,
        "home" to Icons.Filled.Home,
        "school" to Icons.Filled.School,
        "shopping" to Icons.Filled.ShoppingBag,
        "store" to Icons.Filled.Store,
        "travel" to Icons.Filled.Flight,
        "gift" to Icons.Filled.CardGiftcard,
        "heart" to Icons.Filled.Favorite,
        "star" to Icons.Filled.Star,
    )

    fun color(index: Int): Color = colors.getOrElse(index) { colors[0] }
    fun icon(key: String): ImageVector = icons[key] ?: Icons.Filled.AccountBalanceWallet
}

// "₱1,500.00" or "-₱1,500.00"
fun formatBalance(balance: Long): String =
    if (balance < 0) "-₱${formatCents(-balance)}" else "₱${formatCents(balance)}"

@Composable
fun WalletCard(
    modifier: Modifier = Modifier,
    wallet: WalletModel,
    onClick: (() -> Unit)? = null, // null = not clickable
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = WalletStyle.color(wallet.colorIndex), contentColor = Color.White),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clip(MaterialTheme.shapes.extraLarge).clickable(onClick = onClick) else Modifier)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .padding(10.dp)
                ) {
                    Icon(WalletStyle.icon(wallet.iconKey), contentDescription = null)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Skipped when the name already says it (the default "Physical Wallet")
                    val physicalLabel = stringResource(R.string.wallet_lbl_physical)
                    if (wallet.isPhysical && !wallet.name.equals(physicalLabel, ignoreCase = true)) {
                        Text(
                            physicalLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                stringResource(R.string.wallet_lbl_balance),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatBalance(wallet.balance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (wallet.balance < 0) {
                    NegativeChip(modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

// Shown next to a balance below ₱0, readable on any card color
@Composable
fun NegativeChip(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.wallet_lbl_negative),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * Name, balance, color and icon of a wallet. Used inline on the setup screen and inside the add/edit sheet.
 * The balance field only takes positive numbers, so the "negative" switch holds the sign.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WalletEditorFields(
    modifier: Modifier = Modifier,
    draft: WalletModel,
    onDraftChange: (WalletModel) -> Unit,
) {
    val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    // Keyed on the doc so opening another wallet resets it. A ₱0 balance has no sign, so the switch keeps it here.
    var isNegative by rememberSaveable(draft.docId) { mutableStateOf(draft.balance < 0) }

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

        Spacer(modifier = Modifier.height(16.dp))
        FieldLabel(stringResource(R.string.wallet_lbl_color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
        onDismiss = { showCloseDialog = false },
        onConfirm = {
            showCloseDialog = false
            onCloseWallet?.invoke()
        },
    )
}

/** "Close this wallet?" confirm, shared by the editor sheet and the details screen. */
@Composable
fun CloseWalletDialog(
    showDialog: Boolean,
    walletName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    CustomAlertDialog(
        showDialog = showDialog,
        onDismissRequest = onDismiss,
        onConfirmation = onConfirm,
        dialogTitle = stringResource(R.string.wallet_dialog_close_title),
        dialogText = stringResource(R.string.wallet_dialog_close_text, walletName),
        icon = Icons.Filled.AccountBalanceWallet,
        confirmText = stringResource(R.string.wallet_btn_close_wallet),
        dismissText = stringResource(R.string.lbl_cancel),
        confirmColor = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Preview
@Composable
private fun WalletCardPreview() {
    DenuSpendTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                WalletCard(wallet = WalletModel(docId = "uid_physical", name = "Physical Wallet", balance = 150000))
                Spacer(modifier = Modifier.height(8.dp))
                WalletCard(wallet = WalletModel(name = "Credit Card", colorIndex = 7, iconKey = "card", balance = -320050))
            }
        }
    }
}
