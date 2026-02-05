package com.gmail.denuelle42.denuspend.utils.security

import androidx.datastore.core.Serializer
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val token : String? = null
)

