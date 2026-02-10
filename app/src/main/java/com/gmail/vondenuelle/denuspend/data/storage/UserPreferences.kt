package com.gmail.vondenuelle.denuspend.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val uid : String? = null,
    val name : String? = null,
    val email : String? = null,
    val isEmailVerified : Boolean? = false,

)

