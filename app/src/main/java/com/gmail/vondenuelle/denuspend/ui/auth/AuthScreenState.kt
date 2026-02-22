package com.gmail.vondenuelle.denuspend.ui.auth

import com.gmail.vondenuelle.denuspend.domain.models.UserModel

data class AuthScreenState(
    val name : String = "",
    val nameError : String = "",
    val email : String = "",
    val emailError : String = "",
    val password : String = "",
    val passwordError : String = "",

    val showPassword : Boolean = false,

    val isLoading : Boolean = false,
    val showForgotPasswordDialog : Boolean = false,

    val forgotPassEmail : String = "",
    val forgotPassEmailError : String = "",

    val shouldRememberMe : Boolean = false,
    val isSigningIn : Boolean = false,
    val isSigningUp : Boolean = false,
    val userModel : UserModel? = null,

)