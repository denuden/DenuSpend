package com.gmail.vondenuelle.denuspend.data.remote.error


class InvalidCredentialsException(
    override val message: String = "Invalid credentials"
) : Exception(message)

class CannotLogoutException(
    override val message: String = "Cannot logout user"
) : Exception(message)


class NoUserException(
    override val message: String = "No user is signed in"
) : Exception(message)
