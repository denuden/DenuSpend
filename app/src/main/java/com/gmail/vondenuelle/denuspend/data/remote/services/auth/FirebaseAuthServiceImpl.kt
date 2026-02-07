package com.gmail.vondenuelle.denuspend.data.remote.services.auth

import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.LoginRequest
import com.gmail.vondenuelle.denuspend.data.repositories.auth.request.RegisterRequest
import com.gmail.vondenuelle.denuspend.domain.models.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthServiceImpl @Inject constructor(
 private val firebaseAuth : FirebaseAuth
) : AuthService{

    override suspend fun login(request: LoginRequest) : UserModel {
        try {
            // await will suspend until the Firebase task completes
            val result = firebaseAuth.signInWithEmailAndPassword(request.email, request.password)
                .await()  // <-- suspends until complete

            // After await, the task is successful
            val user = result.user ?: throw Exception("User is null after login")
            return UserModel(
                email = user.email,
                isEmailVerified = user.isEmailVerified
            )
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw Exception()
//            throw InvalidCredentialsException(e.message ?: "Invalid credentials")
        } catch (e: Exception) {
            // Other errors
            throw Exception()
        }
    }


    override suspend fun logout() {
//        TODO("Not yet implemented")
    }

    override suspend fun register(request: RegisterRequest) {
//        TODO("Not yet implemented")
    }

    override suspend fun hasUser() {
//        TODO("Not yet implemented")
    }

    override suspend fun getCurrentUser() {
//        TODO("Not yet implemented")
    }

    override suspend fun sendEmailVerification(email: String) {
//        TODO("Not yet implemented")
    }

    override suspend fun sendPasswordReset(email: String) {
//        TODO("Not yet implemented")
    }
}