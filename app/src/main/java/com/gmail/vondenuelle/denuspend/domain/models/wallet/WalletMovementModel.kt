package com.gmail.vondenuelle.denuspend.domain.models.wallet

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

// Money that went in or out of a wallet that isn't an expense: salary, an emergency, a transfer, a balance correction…
// Lives in wallet_movements (not transactions), so it never touches the day/month totals or the allowance math.
@Keep
data class WalletMovementModel(
    @get:Exclude //excludes this from getting stored in firebase
    var docId : String = "",
    val userId : String = "",
    val walletId : String = "",
    val amount : Long = 0L, // cents, + = money in, - = money out
    val title : String = "",
    val presetKey : String = "", // key in WalletMovementPresets, "" = a title the user typed
    val note : String = "",
    val date : Timestamp = Timestamp.now(),
    // Transfers only: the wallet on the other side, and the id both halves share ("{transferId}_out" / "{transferId}_in")
    val otherWalletId : String = "",
    val transferId : String = "",
    // Auto charges only: the subscription / installment it came from (its doc id is "{recurringId}_{yyyyMMdd}")
    val recurringId : String = "",
) {
    @get:Exclude
    val isTransfer : Boolean
        get() = transferId.isNotEmpty()

    companion object {
        // Preset keys the service writes itself (the rest of the presets are UI only, see WalletMovementPresets)
        const val PRESET_CORRECTION = "correction"
        const val PRESET_TRANSFER = "transfer"
        // Card charges the app posts on its own (subscriptions / installments). The Statement screen groups by them.
        const val PRESET_SUBSCRIPTION = "subscription"
        const val PRESET_INSTALLMENT = "installment"

        // Budget's money in / out: the preset keys that are real money coming in or going out ("" = a typed title).
        // Same keys as WalletMovementPresets (stored, never renamed). Left out on purpose: transfer, card_payment, correction
        // and withdrawn (bank → cash), which only move money between your own wallets. Max 30 for whereIn.
        val MONEY_PRESETS = listOf(
            "", PRESET_SUBSCRIPTION, PRESET_INSTALLMENT,
            "salary", "gift", "refund", "sold", "cashback",
            "emergency", "lent", "lost", "annual_fee", "interest", "late_fee",
        )
    }
}
