package com.gmail.vondenuelle.denuspend.data.remote.models.wallet.request

// amount is signed: + = money in, - = money out. toWalletId set = a transfer (amount is then the positive amount moved).
data class WalletMovementRequest(
    val walletId : String,
    val amount : Long,
    val title : String,
    val presetKey : String = "",
    val note : String = "",
    val toWalletId : String = "",
)
