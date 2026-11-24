package com.gmail.vondenuelle.denuspend.ui.wallet.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel

data class MovementPreset(val key: String, @StringRes val label: Int, val icon: ImageVector)

// Shortcut chips in the money in/out sheet. Tapping one fills the title; the key is saved only for the icon.
// Keys are stored in Firestore, so don't rename them.
object WalletMovementPresets {
    val moneyIn = listOf(
        MovementPreset("salary", R.string.wallet_movement_preset_salary, Icons.Filled.Work),
        MovementPreset("gift", R.string.wallet_movement_preset_gift, Icons.Filled.CardGiftcard),
        MovementPreset("refund", R.string.wallet_movement_preset_refund, Icons.Filled.Replay),
        MovementPreset("sold", R.string.wallet_movement_preset_sold, Icons.Filled.Sell),
    )
    val moneyOut = listOf(
        MovementPreset("emergency", R.string.wallet_movement_preset_emergency, Icons.Filled.LocalHospital),
        MovementPreset("lent", R.string.wallet_movement_preset_lent, Icons.Filled.VolunteerActivism),
        MovementPreset("lost", R.string.wallet_movement_preset_lost, Icons.Filled.SearchOff),
        MovementPreset("withdrawn", R.string.wallet_movement_preset_withdrawn, Icons.Filled.LocalAtm),
    )
    // Credit cards: "Pay card" and "Other charge" (fees that aren't expenses). "refund" is the same key as the regular one.
    val cardIn = listOf(
        MovementPreset(PRESET_CARD_PAYMENT, R.string.wallet_movement_preset_card_payment, Icons.Filled.Payments),
        MovementPreset("refund", R.string.wallet_movement_preset_refund, Icons.Filled.Replay),
        MovementPreset("cashback", R.string.wallet_movement_preset_cashback, Icons.Filled.Redeem),
    )
    val cardOut = listOf(
        MovementPreset("annual_fee", R.string.wallet_movement_preset_annual_fee, Icons.Filled.CalendarMonth),
        MovementPreset("interest", R.string.wallet_movement_preset_interest, Icons.Filled.Percent),
        MovementPreset("late_fee", R.string.wallet_movement_preset_late_fee, Icons.Filled.Schedule),
    )

    const val PRESET_CARD_PAYMENT = "card_payment"

    // Money in on a card that the bank gives back, so it never comes "from" another wallet
    fun isFromBank(key: String) = key == "refund" || key == "cashback"

    // null = a typed title, which gets the generic in/out icon
    fun icon(key: String): ImageVector? = when (key) {
        WalletMovementModel.PRESET_CORRECTION -> Icons.Filled.Tune
        WalletMovementModel.PRESET_TRANSFER -> Icons.Filled.SwapHoriz
        else -> (moneyIn + moneyOut + cardIn + cardOut).firstOrNull { it.key == key }?.icon
    }
}
