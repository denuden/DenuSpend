package com.gmail.vondenuelle.denuspend.data.remote.services.auth

import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel

    interface AuthService {
        suspend fun login(request: LoginRequest) : UserModel
        suspend fun logout()
        suspend fun register(request: RegisterRequest)
        suspend fun hasUser()
        suspend fun getCurrentUser()
        suspend fun sendEmailVerification(email : String)
        suspend fun sendPasswordReset(email : String)
    }