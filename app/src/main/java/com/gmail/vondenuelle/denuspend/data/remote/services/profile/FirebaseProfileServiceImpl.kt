package com.gmail.vondenuelle.denuspend.data.remote.services.profile

import androidx.core.net.toUri
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotSendEmailVerification
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotUpdateDetailsException
import com.gmail.vondenuelle.denuspend.data.remote.error.InvalidCredentialsException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateEmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdatePasswordRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateProfileRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.userProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProfileServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : ProfileService {
    override suspend fun getUserProfile(): UserModel {
        try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                return UserModel(
                    uid = user.uid,
                    name = user.displayName,
                    email = user.email,
                    isEmailVerified = user.isEmailVerified
                )
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw InvalidCredentialsException(e.localizedMessage ?: "Cannot get current user")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun updateUserProfile(request: UpdateProfileRequest) {
        try {
            val user = firebaseAuth.currentUser

            val profileUpdates = userProfileChangeRequest {
                displayName = request.name
                photoUri = request.photoUri.orEmpty().toUri()
            }
            if (user != null){
                user.updateProfile(profileUpdates)
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotUpdateDetailsException(e.localizedMessage ?: "Cannot update user details")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun updateEmail(request: UpdateEmailRequest) {
        try {
            val user = firebaseAuth.currentUser
            if (user != null){
                user.verifyBeforeUpdateEmail(request.email, ActionCodeSettings.newBuilder().apply {
                    handleCodeInApp = true
                }.build())
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotUpdateDetailsException(e.localizedMessage ?: "Cannot update user details")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun updatePassword(request: UpdatePasswordRequest) {
        try {
            val user = firebaseAuth.currentUser

            if (user != null){
                user.updatePassword(request.newPassword)
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotUpdateDetailsException(e.localizedMessage ?: "Cannot update user details")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun sendEmailVerification(request: UpdateEmailRequest) {
        try {
            val user = firebaseAuth.currentUser

            if (user != null){
                user.sendEmailVerification(ActionCodeSettings.newBuilder().apply {
                    handleCodeInApp = true
                }.build())
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotSendEmailVerification(e.localizedMessage ?: "Send email verification failed")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun sendPasswordReset(request: UpdateEmailRequest) {
        try {
            val user = firebaseAuth.currentUser

            if (user != null){
                firebaseAuth.sendPasswordResetEmail(request.email, ActionCodeSettings.newBuilder().apply {
                    handleCodeInApp = true
                }.build())
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotSendEmailVerification(e.localizedMessage ?: "Send email verification failed")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }
}