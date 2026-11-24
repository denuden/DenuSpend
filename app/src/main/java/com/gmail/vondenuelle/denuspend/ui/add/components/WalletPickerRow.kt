package com.gmail.vondenuelle.denuspend.ui.add.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.ui.wallet.components.WalletStyle
import com.gmail.vondenuelle.denuspend.ui.wallet.components.MiniOwedLabel
import com.gmail.vondenuelle.denuspend.ui.wallet.components.miniBalanceText

/**
 * "Paid from" as a sideways row of mini wallet cards, then "Don't deduct" ("" = no wallet) unless showNoWalletOption is false.
 * An id that's no longer in the list (closed wallet, e.g. when editing an old expense) gets a selected "Closed wallet" card first.
 */
@Composable
fun WalletPickerRow(
    modifier: Modifier = Modifier,
    wallets: List<WalletModel>,
    hasLoadedWallets: Boolean,
    selectedId: String,
    onSelected: (String) -> Unit,
    showNoWalletOption: Boolean = true, // false for a transfer's "Move to", which always needs a wallet
) {
    val isClosedWallet = hasLoadedWallets && selectedId.isNotEmpty() && wallets.none { it.docId == selectedId }

    // Edge-to-edge row: the content padding keeps the first card in line with the form, and cards scroll under the screen edge
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
    ) {
        // Grey placeholders until the list arrives, so "Don't deduct" never looks picked before the wallets load
        if (!hasLoadedWallets) {
            items(2) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.width(140.dp).height(80.dp)
                ) {}
            }
            return@LazyRow
        }
        if (isClosedWallet) {
            item {
                PlainWalletOption(
                    icon = Icons.Outlined.NoAccounts,
                    label = stringResource(R.string.wallet_lbl_closed_wallet),
                    isSelected = true,
                    onClick = {}
                )
            }
        }
        items(wallets, key = { it.docId }) { wallet ->
            MiniWalletCard(wallet = wallet, isSelected = wallet.docId == selectedId, onClick = { onSelected(wallet.docId) })
        }
        if (showNoWalletOption) {
            item {
                PlainWalletOption(
                    icon = Icons.Outlined.MoneyOff,
                    label = stringResource(R.string.wallet_lbl_no_wallet),
                    isSelected = selectedId.isEmpty(),
                    onClick = { onSelected("") }
                )
            }
        }
    }
}

// Same colors and icon as the wallet's real card, so it's recognizable at a glance.
// A border alone is hard to see on dark cards, so the picked one also gets a check and the others fade.
@Composable
private fun MiniWalletCard(wallet: WalletModel, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = WalletStyle.color(wallet.colorIndex),
        contentColor = Color.White,
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .width(140.dp)
            .height(80.dp)
            .alpha(if (isSelected) 1f else 0.6f)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Icon(WalletStyle.icon(wallet.iconKey), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.weight(1f))
                MiniOwedLabel(wallet, modifier = Modifier.padding(end = 4.dp))
                if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(
                    wallet.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    miniBalanceText(wallet),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

// Outlined option for "Don't deduct" and "Closed wallet", which have no card color
@Composable
private fun PlainWalletOption(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (isSelected) 3.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .width(120.dp)
            .height(80.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}

@Preview
@Composable
private fun WalletPickerRowPreview() {
    DenuSpendTheme {
        Surface {
            WalletPickerRow(
                wallets = listOf(
                    WalletModel(docId = "u_physical", name = "Physical Wallet", colorIndex = 0, iconKey = "wallet", balance = 50000),
                    WalletModel(docId = "g", name = "GCash", colorIndex = 1, iconKey = "ewallet", balance = 120000),
                ),
                hasLoadedWallets = true,
                selectedId = "u_physical",
                onSelected = {},
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
