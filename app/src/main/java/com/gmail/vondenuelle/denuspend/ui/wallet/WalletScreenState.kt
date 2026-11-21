package com.gmail.vondenuelle.denuspend.ui.wallet

import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel

data class WalletScreenState(
    val isLoading : Boolean = false, // saving / closing (full-screen LoadingDialog)
    val isListLoading : Boolean = false,

    val wallets : List<WalletModel> = emptyList(),
    val hasLoadedWallets : Boolean = false,
    // The wallet open in the add/edit sheet (null = closed, empty docId = a new one)
    val walletDraft : WalletModel? = null,

//    Wallet details: expenses paid from that wallet
    val walletTransactions : List<TransactionModel> = emptyList(),
    val isTransactionsLoading : Boolean = false,
    val isLoadingMoreTransactions : Boolean = false,
    val hasMoreTransactions : Boolean = false,
    val hasTransactionsError : Boolean = false, // e.g. a missing index, so the list says it failed instead of "no expenses"

//    First-time setup (0 = physical wallet, 1 = add more wallets)
    val setupStep : Int = 0,
    val physicalDraft : WalletModel = WalletModel(iconKey = "wallet"),
)
