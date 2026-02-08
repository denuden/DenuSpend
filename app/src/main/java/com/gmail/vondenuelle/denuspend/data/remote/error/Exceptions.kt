package com.gmail.vondenuelle.denuspend.data.remote.error


class InvalidCredentialsException(
    override val message: String = "Invalid credentials"
) : Exception(message)
