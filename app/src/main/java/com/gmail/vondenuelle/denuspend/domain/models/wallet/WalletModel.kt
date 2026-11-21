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
) {
    // The physical wallet's doc id is "{uid}_physical", so the id itself says which one it is (no extra field to keep in sync)
    @get:Exclude
    val isPhysical : Boolean
        get() = docId.endsWith(PHYSICAL_SUFFIX)

    companion object {
        const val PHYSICAL_SUFFIX = "_physical"
    }
}
