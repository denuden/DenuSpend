package com.gmail.vondenuelle.denuspend.domain.usecase.auth

import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateProfileRequest
import com.gmail.vondenuelle.denuspend.data.repositories.AuthRepository
import com.gmail.vondenuelle.denuspend.data.repositories.ProfileRepository
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject


@ViewModelScoped
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) {
   suspend fun invoke(registerRequest: RegisterRequest) : UserModel{
        authRepository.register(registerRequest)
        profileRepository.updateUserProfile(
            UpdateProfileRequest(
                name = registerRequest.name,
                email = registerRequest.email
            )
        )
       // register() cached the user before the name was set, so re-read it to save the name to DataStore
       return profileRepository.getUserProfile()
    }
}