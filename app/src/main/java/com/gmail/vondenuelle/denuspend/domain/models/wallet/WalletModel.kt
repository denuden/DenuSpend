package com.gmail.vondenuelle.denuspend.domain.models.wallet

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

@Keep
data class WalletModel(
    @get:Exclude //excludes this from getting stored in firebase
    var docId : String = "",
    val userId : String = "",
    val name : String = "",
    val colorIndex : Int = 0, // index in WalletStyle.colors
    val iconKey : String = "", // key in WalletStyle.icons
    val balance : Long = 0L, // cents, negative = overspent / owed
    val createdAt : Timestamp = Timestamp.now(),
    // Credit cards: "" = a regular wallet. A String, not a Boolean, because Firestore maps a Kotlin "isX" Boolean to the field "x".
    val type : String = "",
    val creditLimit : Long = 0L, // cents, 0 = no limit set (the bar is hidden)
    val statementDay : Int = 0, // 1–31, the day the bank closes each statement (29–31 fall on the month's last day)
    val paymentDueDays : Int = 0, // 1–25 days after the statement day, 0 = not set. Only for the due warning, never for the statement math.
) {
    // The physical wallet's doc id is "{uid}_physical", so the id itself says which one it is (no extra field to keep in sync)
    @get:Exclude
    val isPhysical : Boolean
        get() = docId.endsWith(PHYSICAL_SUFFIX)

    @get:Exclude
    val isCreditCard : Boolean
        get() = type == TYPE_CREDIT_CARD

    // A card's balance is negative while money is owed, so owed is shown as a positive amount
    @get:Exclude
    val owed : Long
        get() = if (balance < 0) -balance else 0L

    // The red "!" shows on a negative wallet, but on a card only when it's over its limit (owing is normal there)
    @get:Exclude
    val needsAttention : Boolean
        get() = if (isCreditCard) creditLimit > 0 && owed > creditLimit else balance < 0

    companion object {
        const val PHYSICAL_SUFFIX = "_physical"
        const val TYPE_CREDIT_CARD = "credit_card"
    }
}
