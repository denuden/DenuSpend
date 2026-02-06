package com.gmail.denuelle42.denuspend.ui.auth

sealed class AuthScreenEvents {
    data class OnChangeEmailField(val value: String) : AuthScreenEvents()
    data class OnChangePasswordField(val value: String) : AuthScreenEvents()
    data class OnChangeRememberMeCheckBox(val value: Boolean) : AuthScreenEvents()
}