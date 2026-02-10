package com.gmail.vondenuelle.denuspend.data.repositories.auth

import android.util.Log
import androidx.datastore.core.DataStore
import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.auth.AuthService
import com.gmail.vondenuelle.denuspend.data.storage.UserPreferences
import com.gmail.vondenuelle.denuspend.di.modules.TokenProvider
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseAuth
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.first
import javax.inject.Inject

//Repository = all the ways the app fetches, persists, or cleans data
@ViewModelScoped
class AuthRepository @Inject constructor(
    @FirebaseAuth private val authService: AuthService,
    private val dataStore: DataStore<UserPreferences>,
    ) {
    suspend fun login(request: LoginRequest) : UserModel{
        val response = authService.login(request)
        dataStore.updateData {
            UserPreferences(
                uid = response.uid,
                name = response.name,
                email = response.email,
                isEmailVerified = response.isEmailVerified
            )
        }
        return response
    }

    suspend fun getCurrentUser() : UserModel {
        val response = authService.getCurrentUser()
        dataStore.updateData {
            UserPreferences(
                uid = response.uid,
                name = response.name,
                email = response.email,
                isEmailVerified = response.isEmailVerified
            )
        }
        return response
    }

    suspend fun logout() {
        authService.logout()
        dataStore.updateData {
            UserPreferences()
        }
    }
}