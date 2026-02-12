package com.gmail.vondenuelle.denuspend.data.remote.services.auth

import android.util.Log
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotLogoutException
import com.gmail.vondenuelle.denuspend.data.remote.error.CannotSendEmailVerification
import com.gmail.vondenuelle.denuspend.data.remote.error.InvalidCredentialsException
import com.gmail.vondenuelle.denuspend.data.remote.error.NoUserException
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthService {

    override suspend fun login(request: LoginRequest): UserModel {
        try {
            // await will suspend until the Firebase task completes
            val result = firebaseAuth.signInWithEmailAndPassword(request.email, request.password)
                .await()

            val user = result.user ?: throw Exception("User is null after login")
            return UserModel(
                uid = user.uid,
                name = user.displayName,
                email = user.email,
                isEmailVerified = user.isEmailVerified
            )
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            Log.e("Firebase", e.toString())
            throw InvalidCredentialsException(e.message ?: "Invalid credentials")
        } catch (e: Exception) {
            Log.e("Firebase", e.toString())

            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun register(request: RegisterRequest): UserModel {
        try {
            // await will suspend until the Firebase task completes
            val result =
                firebaseAuth.createUserWithEmailAndPassword(request.email, request.password)
                    .await()

            val user = result.user ?: throw Exception("User is null after login")
            return UserModel(
                uid = user.uid,
                name = user.displayName,
                email = user.email,
                isEmailVerified = user.isEmailVerified
            )
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw InvalidCredentialsException(e.localizedMessage ?: "Invalid credentials")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }


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

    override suspend fun hasUser(): Boolean {
        return try {
            val user = firebaseAuth.currentUser
            user != null
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw InvalidCredentialsException(e.localizedMessage ?: "Cannot get current user")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    override suspend fun getCurrentUser(): UserModel {
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

    override suspend fun reAuthenticateUser(request: LoginRequest) {
        try {
            val user = firebaseAuth.currentUser

            if (user != null){
                val credential = EmailAuthProvider
                    .getCredential(request.email, request.password)
                // Prompt the user to re-provide their sign-in credentials
                user.reauthenticate(credential)
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw InvalidCredentialsException(e.localizedMessage ?: "Invalid credentials")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }
}