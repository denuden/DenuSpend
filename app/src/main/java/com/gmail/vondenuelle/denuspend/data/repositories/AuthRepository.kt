package com.gmail.vondenuelle.denuspend.data.repositories

import androidx.datastore.core.DataStore
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.EmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.auth.AuthService
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.data.storage.UserPreferences
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseAuth
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

//Repository = Regarding about the backend response
// provides abstraction to service
//one is to one to the service
//this will be the source of truth for local or remote
//cleans data that you need
@ViewModelScoped
class AuthRepository @Inject constructor(
    @FirebaseAuth private val authService: AuthService,
    private val dataStore: DataStore<UserPreferences>,
) {
    suspend fun login(request: LoginRequest): UserModel {
        val user = authService.login(
            request.copy(
                email = request.email.trim(),
                password = request.password.trim()
            )
        )
        dataStore.updateData {
            UserPreferences(
                uid = user.uid,
                name = user.name,
                email = user.email,
                photo = user.photo,
                isEmailVerified = user.isEmailVerified
            )
        }
        return user
    }

    suspend fun register(request: RegisterRequest): UserModel {
        val user = authService.register(
            request.copy(
                email = request.email.trim(),
                password = request.password.trim()
            )
        )
        dataStore.updateData {
            UserPreferences(
                uid = user.uid,
                name = user.name,
                email = user.email,
                photo = user.photo,
                isEmailVerified = user.isEmailVerified
            )
        }
        return user
    }

    suspend fun getCurrentUser(): UserModel {
        val user = authService.getCurrentUser()
        dataStore.updateData {
            UserPreferences(
                uid = user.uid,
                name = user.name,
                email = user.email,
                photo = user.photo,
                isEmailVerified = user.isEmailVerified
            )
        }
        return user
    }

    suspend fun sendPasswordReset(request: EmailRequest) {
        authService.sendPasswordReset(request)
    }
}