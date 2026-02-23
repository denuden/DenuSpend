package com.gmail.vondenuelle.denuspend.data.remote.services.profile

import android.util.Log
import androidx.core.net.toUri
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotLogoutException
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotSendEmailVerification
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotUpdateDetailsException
import com.gmail.vondenuelle.denuspend.data.remote.error.InvalidCredentialsException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.error.ReAuthenticateException
import com.gmail.vondenuelle.denuspend.data.remote.error.WeakPasswordException
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateEmailRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdatePasswordRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.profile.request.UpdateProfileRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import com.google.firebase.FirebaseException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProfileServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : ProfileService {
    override suspend fun logout() {
        try {
            firebaseAuth.signOut()
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotLogoutException(e.localizedMessage.orEmpty())
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }


    override suspend fun getUserProfile(): UserModel {
        try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                return UserModel(
                    uid = user.uid,
                    name = user.displayName,
                    email = user.email,
                    isEmailVerified = user.isEmailVerified,
                    photo = user.photoUrl.toString()
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
            if (user != null) {
                user.updateProfile(profileUpdates).await()
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
            if (user != null) {
                val actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl("https://denu-spend.firebaseapp.com")
                    .setHandleCodeInApp(true)
                    .setAndroidPackageName(
                        "com.gmail.vondenuelle.denuspend.debug",
                        true,
                        null
                    )
                    .build()

                user.verifyBeforeUpdateEmail(request.email, actionCodeSettings).await()
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

            if (user != null) {
                user.updatePassword(request.newPassword).await()
            } else {
                throw NoUserException()
            }
        } catch (e : FirebaseAuthRecentLoginRequiredException){
            throw ReAuthenticateException(
                e.localizedMessage ?: "Credentials required to do certain actions"
            )
        }
        catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotUpdateDetailsException(e.localizedMessage ?: "Cannot update user details")
        } catch (e: FirebaseException) {
            if (e.localizedMessage?.contains("PASSWORD_DOES_NOT_MEET_REQUIREMENTS") == true) {
                throw WeakPasswordException("Password must contain at least 8 characters, Password must contain an upper case character, Password must contain a numeric character, Password must contain a non-alphanumeric character")
            } else {
                throw Exception(e.localizedMessage ?: "Unknown Firebase Exception")
            }
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun sendEmailVerification() {
        try {
            val user = firebaseAuth.currentUser

            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setUrl("https://denu-spend.firebaseapp.com")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(
                    "com.gmail.vondenuelle.denuspend.debug",
                    true,
                    null
                )
                .build()

            if (user != null) {
                user.sendEmailVerification(actionCodeSettings).await()
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotSendEmailVerification(
                e.localizedMessage ?: "Send email verification failed"
            )
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun deleteAccount() {
        try {
            val user = firebaseAuth.currentUser

            if (user != null) {
                user.delete().await()
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotSendEmailVerification(
                e.localizedMessage ?: "Send email verification failed"
            )
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun reauthenticateUser(request: LoginRequest) {
        try {
            val user = firebaseAuth.currentUser

            if (user != null) {
                // Get auth credentials from the user for re-authentication. The example below shows
                // email and password credentials but there are multiple possible providers,
                // such as GoogleAuthProvider or FacebookAuthProvider.
                val credential = EmailAuthProvider
                    .getCredential(request.email, request.password)

                // Prompt the user to re-provide their sign-in credentials
                user.reauthenticate(credential).await()
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Firebase-specific errors
            throw InvalidCredentialsException(
                e.localizedMessage ?: "Invalid credentials"
            )
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }
}