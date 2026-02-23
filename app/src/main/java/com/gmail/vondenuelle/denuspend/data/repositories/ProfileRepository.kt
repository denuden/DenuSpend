package com.gmail.vondenuelle.denuspend.data.repositories

import androidx.datastore.core.DataStore
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotUpdateDetailsException
import com.gmail.vondenuelle.denuspend.data.remote.error.PasswordDoNotMatchException
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateEmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdatePasswordRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateProfileRequest
import com.gmail.vondenuelle.denuspend.data.remote.services.profile.ProfileService
import com.gmail.vondenuelle.denuspend.data.storage.UserPreferences
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseProfile
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ProfileRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
    @FirebaseProfile private val profileService: ProfileService
) {
    suspend fun getUserProfile(): UserModel {
        val user = profileService.getUserProfile()
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

    suspend fun updateUserProfile(request: UpdateProfileRequest) {
        if (request.name.orEmpty().isEmpty()) {
            throw CannotUpdateDetailsException("Name cannot be empty")
        }
        profileService.updateUserProfile(request)
    }


    suspend fun sendEmailVerification() {
        profileService.sendEmailVerification()
    }

    suspend fun deleteAccount() {
        profileService.deleteAccount()
        dataStore.updateData {
            UserPreferences()
        }
        profileService.logout()
    }

    suspend fun logout() {
        profileService.logout()
        dataStore.updateData {
            UserPreferences()
        }
    }

    suspend fun updatePassword(request: UpdatePasswordRequest) {
        if (request.reEnterPassword.trim() != request.newPassword.trim()) {
            throw PasswordDoNotMatchException()
        }
        profileService.updatePassword(
            request.copy(
                newPassword = request.newPassword.trim(),
                reEnterPassword = request.reEnterPassword.trim()
            )
        )
    }

    suspend fun updateEmail(request: UpdateEmailRequest) {
        profileService.updateEmail(request)
    }
    suspend fun reauthenticateUser(request: LoginRequest) {
        profileService.reauthenticateUser(request.copy(
            email = request.email.trim(),
            password = request.password.trim()
        ))
    }
}