package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.services.profile.ProfileService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseProfile
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ProfileRepository @Inject constructor(
    @FirebaseProfile private val profileService: ProfileService
) {
    suspend fun getUserProfile() : UserModel {
        val user = profileService.getUserProfile()
        return user
    }
}