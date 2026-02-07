package com.gmail.vondenuelle.denuspend.utils.security

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val token : String? = null
)

