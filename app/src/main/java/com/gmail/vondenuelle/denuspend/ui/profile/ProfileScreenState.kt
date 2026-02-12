package com.gmail.vondenuelle.denuspend.ui.profile

import com.gmail.vondenuelle.denuspend.domain.models.UserModel

data class ProfileScreenState(
    val isLoading: Boolean = false,
    val profile: UserModel? = null,

    //for fields
    val name: String = "",
    val nameError: String = "",
    val photo: String = "",
    val photoError: String = "",
)
