package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.gmail.vondenuelle.denuspend.utils.CurrencyUtils.formatCents

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
