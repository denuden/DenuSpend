package com.gmail.vondenuelle.denuspend.data.remote.models.transaction.response

import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel

data class TransactionOverviewResponse(
    val dailyHistory: DailyHistoryModel? = null,
    val transactions: List<TransactionModel> = emptyList()
)