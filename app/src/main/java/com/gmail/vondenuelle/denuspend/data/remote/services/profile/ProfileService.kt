package com.gmail.vondenuelle.denuspend.data.remote.services.profile

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateEmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdatePasswordRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateProfileRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel

interface ProfileService {
    suspend fun getUserProfile() : UserModel
    suspend fun updateUserProfile(request: UpdateProfileRequest)
    suspend fun updateEmail(request : UpdateEmailRequest)
    suspend fun updatePassword(request : UpdatePasswordRequest)
    suspend fun sendEmailVerification()
    suspend fun deleteAccount()
    suspend fun reauthenticateUser(request: LoginRequest)
    suspend fun logout()
}