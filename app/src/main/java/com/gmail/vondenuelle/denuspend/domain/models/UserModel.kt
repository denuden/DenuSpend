package com.gmail.vondenuelle.denuspend.domain.models

data class UserModel(
    val uid : String ? = null,
    val name : String ? = null,
    val email : String? = null,
    val photo : String? = null,
    val isEmailVerified : Boolean? = false,
)
