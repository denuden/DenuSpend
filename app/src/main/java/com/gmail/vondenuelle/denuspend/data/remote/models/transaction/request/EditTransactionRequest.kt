package com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request

data class EditTransactionRequest(
    val docId : String,
    val dailyDocId : String,
    val oldAmount : Long, // needed to move the daily totals by the difference
    val oldCategory : String, // needed to move the per-category daily totals when the category changes
    val title : String,
    val description : String,
    val amount : Long = 0L,
    val category : String,
    val walletId : String = "",
    val oldWalletId : String = "", // needed to refund the old wallet when the wallet changes
)
