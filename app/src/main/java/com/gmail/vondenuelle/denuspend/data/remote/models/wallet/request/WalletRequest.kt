package com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request

// docId empty = a new wallet
data class WalletRequest(
    val docId : String = "",
    val name : String,
    val colorIndex : Int,
    val iconKey : String,
    val balance : Long,
)
