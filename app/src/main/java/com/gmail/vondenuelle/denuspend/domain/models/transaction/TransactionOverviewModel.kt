package com.gmail.vondenuelle.denuspend.domain.models.transaction

import androidx.annotation.Keep

@Keep
data class TransactionOverviewModel(
    val dailyHistory: DailyHistoryModel = DailyHistoryModel(),
    val transactions: List<TransactionModel> = emptyList()
)