package com.gmail.vondenuelle.denuspend.ui.home

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest

sealed class HomeScreenEvents {
    object OnGetCurrentUser  : HomeScreenEvents()
    object OnSignOut  : HomeScreenEvents()
    object OnGetToday : HomeScreenEvents()
    object OnGetMonth : HomeScreenEvents()
    object OnGetWallets : HomeScreenEvents()
    data class OnOpenCategoryInsights(val category : String) : HomeScreenEvents()
    data class OnOpenWalletDetails(val walletId : String) : HomeScreenEvents()
    object OnOpenHistory : HomeScreenEvents()
}
