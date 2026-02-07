package com.gmail.vondenuelle.denuspend.domain.models

data class UserModel(
    val email : String? = null,
    val isEmailVerified : Boolean? = false,
)
