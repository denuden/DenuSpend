package com.gmail.vondenuelle.denuspend.data.remote.models.profile.request

data class UpdateProfileRequest(
    val name : String? = null,
    val photoUri : String? = null, //must use Uri.parse
)
