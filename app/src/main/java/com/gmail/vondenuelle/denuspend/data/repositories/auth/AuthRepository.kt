package com.gmail.vondenuelle.denuspend.data.repositories.auth

import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.auth.AuthService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseAuth
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class AuthRepository @Inject constructor(
    @FirebaseAuth private val authService: AuthService
) {
    suspend fun login(request: LoginRequest) : UserModel{
        return authService.login(request)
    }
}