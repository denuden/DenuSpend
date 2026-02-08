package com.gmail.vondenuelle.denuspend.ui.auth

import com.gmail.vondenuelle.denuspend.domain.models.UserModel

data class AuthScreenState(
    val name : String = "",
    val nameError : String = "",
    val email : String = "",
    val emailError : String = "",
    val password : String = "",
    val passwordError : String = "",

    val shouldRememberMe : Boolean = false,
    val isSigningIn : Boolean = false,
    val userModel : UserModel? = null,

)