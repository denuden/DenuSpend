package com.gmail.vondenuelle.denuspend.ui.auth

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.RegisterRequest

sealed class AuthScreenEvents {
    data class OnChangeEmailField(val value: String) : AuthScreenEvents()
    data class OnChangePasswordField(val value: String) : AuthScreenEvents()
    data class OnChangeNameField(val value: String) : AuthScreenEvents()
    data class OnChangeRememberMeCheckBox(val value: Boolean) : AuthScreenEvents()

    data class OnChangePasswordVisibility(val value: Boolean) : AuthScreenEvents()

    data class OnLoginWithEmailAndPassword(val request : LoginRequest) : AuthScreenEvents()
    data class OnRegisterWithEmailAndPassword(val request : RegisterRequest) : AuthScreenEvents()
    object OnGetCurrentUser : AuthScreenEvents()
    //    Navigation
    object OnNavigateToRegister : AuthScreenEvents()
    object OnNavigateToLogin : AuthScreenEvents()
}