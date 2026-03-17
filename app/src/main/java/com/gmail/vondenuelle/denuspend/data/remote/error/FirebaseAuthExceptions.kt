package com.gmail.vondenuelle.denuspend.data.remote.error


class InvalidCredentialsException(
    override val message: String = "Invalid credentials"
) : Exception(message)

class CannotLogoutException(
    override val message: String = "Cannot logout user"
) : Exception(message)

class CannotUpdateDetailsException(
    override val message: String = "Cannot update user details"
) : Exception(message)

class CannotSendEmailVerification(
    override val message: String = "Cannot update user details"
) : Exception(message)

class NoUserException(
    override val message: String = "No user is signed in"
) : Exception(message)

class PasswordDoNotMatchException(
    override val message: String = "Password do not match"
) : Exception(message)

class WeakPasswordException(
    override val message: String = "Weak password combination"
) : Exception(message)

class ReAuthenticateException(
    override val message: String = "Credentials required to do certain actions"
) : Exception(message)
