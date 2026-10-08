package com.gmail.vondenuelle.denuspend.domain.models

import androidx.annotation.Keep

@Keep
data class UserModel(
    val uid : String = "",
    val name : String = "",
    val email : String = "",
    val photo : String = "",
    val isEmailVerified : Boolean = false,
)
