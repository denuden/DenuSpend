package com.gmail.vondenuelle.denuspend.domain.repositories.auth

import com.gmail.vondenuelle.denuspend.data.repositories.auth.AuthRepository
import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.di.modules.IoDispatcher
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ViewModelScoped
class AuthUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    fun login(request : LoginRequest) : Flow<LoginResponse> {
        return flow {
            val response = authRepository.login(request)
            emit(response)
        }.flowOn(ioDispatcher)
    }
}