package com.gmail.vondenuelle.denuspend.data.remote.services.profile

import android.content.Context
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
import com.gmail.vondenuelle.denuspend.utils.orDefault
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.ALLOWANCE_TEMPLATES_COLLECTION
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.DAILY_HISTORY
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.MONTHLY_HISTORY
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.TRANSACTION
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl.Companion.USER_ID
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseProfileServiceImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFireStore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
) : ProfileService {
    // Email links open this build's app. packageName already has the ".debug" suffix on debug builds.
    private val actionCodeSettings by lazy {
        ActionCodeSettings.newBuilder()
            .setUrl("https://denu-spend.firebaseapp.com")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(context.packageName, true, null)
            .build()
    }

    companion object {
        const val USERS = "users"
        const val NAME = "name"
        const val EMAIL = "email"
        const val CREATED_AT = "createdAt"
    }

    /**
     * Creates users/{uid} if it's missing, or refreshes name/email if it exists (set + merge does both in one write).
     * createdAt comes from the Auth account, so it's the real sign-up date even for accounts backfilled later.
     * Not awaited on purpose: an offline write never completes, which would block login. Firestore sends it once online.
     */
    override fun syncUserDoc() {
        val user = firebaseAuth.currentUser ?: return
        val data = mutableMapOf<String, Any?>(
            NAME to user.displayName,
            EMAIL to user.email,
        )
        user.metadata?.creationTimestamp?.let { data[CREATED_AT] = Timestamp(Date(it)) }

        firebaseFireStore.collection(USERS).document(user.uid)
            .set(data, SetOptions.merge())
            .addOnFailureListener { Log.e("FirebaseProfileService", "syncUserDoc failed", it) }
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


    override suspend fun getUserProfile(): UserModel {
        try {
            // Pulls fresh data from Firebase, e.g. an email verified in a browser. Offline keeps the cached user.
            try {
                firebaseAuth.currentUser?.reload()?.await()
            } catch (_: FirebaseNetworkException) {
            }

            val user = firebaseAuth.currentUser
            if (user != null) {
                return UserModel(
                    uid = user.uid,
                    name = user.displayName.orDefault(),
                    email = user.email.orDefault(),
                    isEmailVerified = user.isEmailVerified,
                    photo = user.photoUrl?.toString().orEmpty()
                )
            } else {
                throw NoUserException()
            }
        } catch (_: FirebaseAuthInvalidUserException) {
            // Account was deleted or disabled elsewhere, so the VM logs out
            throw NoUserException()
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
                user.verifyBeforeUpdateEmail(request.email, actionCodeSettings).await()
            } else {
                throw NoUserException()
            }
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw ReAuthenticateException(
                e.localizedMessage ?: "Credentials required to do certain actions"
            )
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
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw ReAuthenticateException(
                e.localizedMessage ?: "Credentials required to do certain actions"
            )
        } catch (e: FirebaseAuthException) {
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

    override suspend fun deleteAccount(request: LoginRequest) {
        try {
            val user = firebaseAuth.currentUser ?: throw NoUserException()

            // Firebase needs a recent login to delete, so this doubles as the password check
            val credential = EmailAuthProvider.getCredential(request.email, request.password)
            user.reauthenticate(credential).await()

            // Data goes first: once the user is deleted, the security rules block these deletes
            deleteUserData(user.uid)
            user.delete().await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            throw InvalidCredentialsException(e.localizedMessage ?: "Invalid credentials")
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw ReAuthenticateException(
                e.localizedMessage ?: "Credentials required to do certain actions"
            )
        } catch (e: FirebaseAuthException) {
            // Firebase-specific errors
            throw CannotUpdateDetailsException(e.localizedMessage ?: "Cannot delete account")
        } catch (e: Exception) {
            // rethrow error for custom exceptions
            throw e
        }
    }

    // Deletes every daily_history day (plus its transactions subcollection), every monthly_history month, every top-level transaction, users/{uid} and allowance_templates/{uid}.
    // A Firestore batch holds at most 500 writes, so the deletes are committed in chunks.
    private suspend fun deleteUserData(uid: String) {
        val refs = mutableListOf<DocumentReference>()

        val days = firebaseFireStore.collection(DAILY_HISTORY)
            .whereEqualTo(USER_ID, uid)
            .get().await()
        for (day in days.documents) {
            val dayTransactions = day.reference.collection(TRANSACTION).get().await()
            refs += dayTransactions.documents.map { it.reference }
            refs += day.reference
        }

        val months = firebaseFireStore.collection(MONTHLY_HISTORY)
            .whereEqualTo(USER_ID, uid)
            .get().await()
        refs += months.documents.map { it.reference }

        val transactions = firebaseFireStore.collection(TRANSACTION)
            .whereEqualTo(USER_ID, uid)
            .get().await()
        refs += transactions.documents.map { it.reference }
        refs += firebaseFireStore.collection(USERS).document(uid)
        refs += firebaseFireStore.collection(ALLOWANCE_TEMPLATES_COLLECTION).document(uid)

        refs.chunked(500).forEach { chunk ->
            val batch = firebaseFireStore.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
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
            } else {
                throw NoUserException()
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