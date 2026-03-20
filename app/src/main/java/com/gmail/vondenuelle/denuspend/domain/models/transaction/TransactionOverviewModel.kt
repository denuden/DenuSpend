package com.gmail.vondenuelle.denuspend.domain.models.transaction

data class TransactionOverviewModel(
    val dailyHistory: DailyHistoryModel? = null,
    val transactions: List<TransactionModel> = emptyList()
)