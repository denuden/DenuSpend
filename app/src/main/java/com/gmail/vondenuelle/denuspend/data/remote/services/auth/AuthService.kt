package com.gmail.vondenuelle.denuspend.data.remote.services.auth

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel

interface AuthService {
    suspend fun login(request: LoginRequest) : UserModel
    suspend fun logout()
    suspend fun register(request: RegisterRequest) : UserModel
    suspend fun hasUser() : Boolean
    suspend fun getCurrentUser() : UserModel
    suspend fun sendEmailVerification(email : String)
    suspend fun sendPasswordReset(email : String)
}