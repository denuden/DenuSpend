package com.gmail.vondenuelle.denuspend.ui.auth

import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest

sealed class AuthScreenEvents {
    data class OnChangeEmailField(val value: String) : AuthScreenEvents()
    data class OnChangePasswordField(val value: String) : AuthScreenEvents()
    data class OnChangeNameField(val value: String) : AuthScreenEvents()
    data class OnChangeRememberMeCheckBox(val value: Boolean) : AuthScreenEvents()

    data class OnLoginWithEmailAndPassword(val request : LoginRequest) : AuthScreenEvents()
    object OnGetCurrentUser : AuthScreenEvents()
    //    Navigation
    object OnNavigateToRegister : AuthScreenEvents()
    object OnNavigateToLogin : AuthScreenEvents()
}