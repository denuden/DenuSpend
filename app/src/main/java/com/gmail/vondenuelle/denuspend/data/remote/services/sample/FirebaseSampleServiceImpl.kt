package com.gmail.vondenuelle.denuspend.data.remote.services.sample

import com.gmail.vondenuelle.denuspend.data.remote.models.sample.request.GetRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.sample.response.GetResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSampleServiceImpl @Inject constructor(
    //real firebase object from firebase itself
//    private val firebaseAuth : FirebaseAuth
) : SampleService{
    override suspend fun login(request: GetRequest) : GetResponse {
        TODO("Not yet implemented")
    }

    override suspend fun logout() {
        TODO("Not yet implemented")
    }

    override suspend fun register() {
        TODO("Not yet implemented")
    }

    override suspend fun hasUser() {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentUser() {
        TODO("Not yet implemented")
    }

    override suspend fun sendEmailVerification(email: String) {
        TODO("Not yet implemented")
    }

    override suspend fun sendPasswordReset(email: String) {
        TODO("Not yet implemented")
    }
}