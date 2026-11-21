package com.gmail.vondenuelle.denuspend.ui.home

import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import com.gmail.vondenuelle.denuspend.domain.models.budget.MonthlyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.DailyHistoryModel
import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel

data class HomeScreenState(
    val userModel : UserModel? = null,
    val todayHistory : DailyHistoryModel = DailyHistoryModel(), // today's daily doc: totals + per-category maps
    val todayTransactions : List<TransactionModel> = emptyList(), // newest first
    val isTodayLoading : Boolean = true,
    val monthHistory : MonthlyHistoryModel = MonthlyHistoryModel(), // this month's per-category maps
    val isMonthLoading : Boolean = true,
    val wallets : List<WalletModel> = emptyList(), // physical first
    val isWalletsLoading : Boolean = true,
)
