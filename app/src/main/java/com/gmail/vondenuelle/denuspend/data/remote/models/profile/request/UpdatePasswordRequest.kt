package com.gmail.vondenuelle.denuspend.data.remote.models.profile.request

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest

data class UpdatePasswordRequest(
    val newPassword : String,
    val reEnterPassword : String,
)
