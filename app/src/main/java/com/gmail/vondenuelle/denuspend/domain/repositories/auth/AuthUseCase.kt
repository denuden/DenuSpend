package com.gmail.vondenuelle.denuspend.domain.repositories.auth

import com.gmail.vondenuelle.denuspend.data.repositories.auth.AuthRepository
import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

//UseCase = what the app does with that data to satisfy business / UI logic
@ViewModelScoped
class AuthUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    fun login(request : LoginRequest) : Flow<UserModel> {
        return flow {
            val response = authRepository.login(request)

            emit(response)
        }.flowOn(ioDispatcher)
    }

    fun getCurrentUser() : Flow<UserModel> {
        return flow {
            val response = authRepository.getCurrentUser()
            emit(response)
        }.flowOn(ioDispatcher)
    }

    fun logout() : Flow<Boolean> {
        return flow {
            authRepository.logout()
            emit(true)
        }.flowOn(ioDispatcher)
    }
}