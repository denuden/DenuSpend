package com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request

// docId empty = a new wallet
data class WalletRequest(
    val docId : String = "",
    val name : String,
    val colorIndex : Int,
    val iconKey : String,
    val balance : Long,
    // Edits only: the balance when the sheet opened. A different balance is saved as an increment plus a "Balance correction" entry.
    val originalBalance : Long = 0L,
    val correctionTitle : String = "",
)
