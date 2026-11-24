package com.gmail.vondenuelle.denuspend.ui.wallet

import com.gmail.vondenuelle.denuspend.domain.models.transaction.TransactionModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import java.time.LocalDate

data class WalletScreenState(
    val isLoading : Boolean = false, // saving / closing (full-screen LoadingDialog)
    val isListLoading : Boolean = false,

    val wallets : List<WalletModel> = emptyList(),
    val hasLoadedWallets : Boolean = false,
    // The wallet open in the add/edit sheet (null = closed, empty docId = a new one)
    val walletDraft : WalletModel? = null,
    val walletDraftOriginalBalance : Long = 0L, // the balance when the sheet opened, so a change is saved as a correction

//    Wallet details: expenses paid from that wallet
    val walletTransactions : List<TransactionModel> = emptyList(),
    val isTransactionsLoading : Boolean = false,
    val isLoadingMoreTransactions : Boolean = false,
    val hasMoreTransactions : Boolean = false,
    val hasTransactionsError : Boolean = false, // e.g. a missing index, so the list says it failed instead of "no expenses"

//    Wallet details: money in / out that isn't an expense (wallet_movements), merged with the expenses on screen
    val walletMovements : List<WalletMovementModel> = emptyList(),
    val isMovementsLoading : Boolean = false,
    val isLoadingMoreMovements : Boolean = false,
    val hasMoreMovements : Boolean = false,
    val hasMovementsError : Boolean = false,
    val movementDraft : MovementDraft? = null, // the add money / take out / transfer sheet (null = closed)

//    Credit cards: every expense and money in/out on the card from the start of the statement being shown (card details + Statement screen)
    val statementTransactions : List<TransactionModel> = emptyList(),
    val statementMovements : List<WalletMovementModel> = emptyList(),
    val isStatementLoading : Boolean = false,
    val hasLoadedStatement : Boolean = false,
    val hasStatementError : Boolean = false,
    val statementLoadedSince : LocalDate? = null, // the range the statement lists belong to, so a new month never shows the old month's rows

//    First-time setup (0 = physical wallet, 1 = add more wallets)
    val setupStep : Int = 0,
    val physicalDraft : WalletModel = WalletModel(iconKey = "wallet"),
)

enum class MovementType { IN, OUT, TRANSFER }

// amount is always positive here; MovementType gives the sign when it's saved
data class MovementDraft(
    val type : MovementType,
    val amount : Long = 0L,
    val title : String = "",
    val presetKey : String = "",
    val note : String = "",
    val toWalletId : String = "", // transfers only
    val fromWalletId : String = "", // "Pay card" only: the wallet the payment came from ("" = don't deduct from one)
)
