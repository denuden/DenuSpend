package com.gmail.vondenuelle.denuspend.ui.wallet

import com.gmail.vondenuelle.denuspend.domain.models.wallet.WalletModel

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
    data class OnLoadMoreWalletTransactions(val walletId : String) : WalletScreenEvents()

    //First-time setup
    data class OnPhysicalDraftChanged(val draft : WalletModel) : WalletScreenEvents()
    data object OnSavePhysicalWallet : WalletScreenEvents()
    data object OnSetupStepBack : WalletScreenEvents()
    data object OnSignOut : WalletScreenEvents()
}
