package com.gmail.vondenuelle.denuspend.data.remote.models.auth.request

data class RegisterRequest(
    val name : String,
    val email : String,
    val password : String
)