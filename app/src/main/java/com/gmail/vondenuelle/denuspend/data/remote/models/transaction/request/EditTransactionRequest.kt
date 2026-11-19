package com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request

data class EditTransactionRequest(
    val docId : String,
    val dailyDocId : String,
    val oldAmount : Long, // needed to move the daily totals by the difference
    val title : String,
    val description : String,
    val amount : Long = 0L,
    val category : String,
)
