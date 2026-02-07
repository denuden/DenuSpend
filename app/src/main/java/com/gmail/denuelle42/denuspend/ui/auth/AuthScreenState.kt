package com.gmail.denuelle42.denuspend.ui.auth

data class AuthScreenState(
    val name : String = "",
    val nameError : String = "",
    val email : String = "",
    val emailError : String = "",
    val password : String = "",
    val passwordError : String = "",

    val shouldRememberMe : Boolean = false,
    val isLoggingIn : Boolean = false,
)