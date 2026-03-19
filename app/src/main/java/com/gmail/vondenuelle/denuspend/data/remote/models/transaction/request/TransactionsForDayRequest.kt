package com.gmail.vondenuelle.denuspend.data.remote.models.transaction.request

import com.google.firebase.Timestamp

data class TransactionsForDayRequest(
    val dailyDocId: String,
    val limit: Long? = null
)