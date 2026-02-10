package com.gmail.vondenuelle.denuspend.ui.home

import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest

sealed class HomeScreenEvents {
    object OnGetCurrentUser  : HomeScreenEvents()
    object OnSignOut  : HomeScreenEvents()
}