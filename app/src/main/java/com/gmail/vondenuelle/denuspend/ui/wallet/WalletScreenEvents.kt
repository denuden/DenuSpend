package com.gmail.vondenuelle.denuspend.ui.wallet

import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel
import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletMovementModel
import java.time.LocalDate

sealed class WalletScreenEvents {
    data object OnGetWallets : WalletScreenEvents()
    data class OnNavigateToWalletDetails(val walletId : String) : WalletScreenEvents()

    //Add / edit sheet
    data class OnOpenWalletEditor(val wallet : WalletModel?) : WalletScreenEvents() // null = new wallet
    data object OnCloseWalletEditor : WalletScreenEvents()
    data class OnWalletDraftChanged(val draft : WalletModel) : WalletScreenEvents()
    data object OnSaveWalletDraft : WalletScreenEvents()
    data class OnCloseWallet(val wallet : WalletModel) : WalletScreenEvents()

    //Wallet details
    data class OnGetWalletTransactions(val walletId : String) : WalletScreenEvents()
    data class OnGetWalletMovements(val walletId : String) : WalletScreenEvents()
    data class OnLoadMoreWalletTransactions(val walletId : String) : WalletScreenEvents() // loads more of both lists

    //Credit card statements
    data class OnGetStatement(val walletId : String, val since : LocalDate) : WalletScreenEvents() // since = first day of the oldest cycle needed
    data class OnNavigateToStatement(val walletId : String, val monthsBack : Int) : WalletScreenEvents()

    //Money in / out sheet
    data class OnOpenMovementSheet(val type : MovementType, val isCreditCard : Boolean = false) : WalletScreenEvents()
    data object OnCloseMovementSheet : WalletScreenEvents()
    data class OnMovementDraftChanged(val draft : MovementDraft) : WalletScreenEvents()
    data class OnSaveMovement(val wallet : WalletModel) : WalletScreenEvents()
    data class OnDeleteMovement(val movement : WalletMovementModel) : WalletScreenEvents()

    //First-time setup
    data class OnPhysicalDraftChanged(val draft : WalletModel) : WalletScreenEvents()
    data object OnSavePhysicalWallet : WalletScreenEvents()
    data object OnSetupStepBack : WalletScreenEvents()
    data object OnSignOut : WalletScreenEvents()
}
