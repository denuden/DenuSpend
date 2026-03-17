package com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request

data class TransactionRequest(
    val title : String,
    val description : String,
    val amount : Long = 0L,
    val category : String,
)