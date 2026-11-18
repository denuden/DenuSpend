package com.gmail.vondenuelle.denuspend.data.remote.services.auth

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.EmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import kotlinx.coroutines.flow.Flow

interface AuthService {
    suspend fun login(request: LoginRequest) : UserModel
    suspend fun register(request: RegisterRequest) : UserModel
    suspend fun hasUser() : Boolean
    suspend fun getCurrentUser() : UserModel
    suspend fun reAuthenticateUser(request : LoginRequest)
    suspend fun sendPasswordReset(request : EmailRequest)
    fun observeAuthState() : Flow<Boolean>
}