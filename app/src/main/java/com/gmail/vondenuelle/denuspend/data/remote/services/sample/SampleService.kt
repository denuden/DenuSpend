package com.gmail.vondenuelle.denuspend.data.remote.services.sample

import com.gmail.vondenuelle.denuspend.data.repositories.sample.request.GetRequest
import com.gmail.vondenuelle.denuspend.data.repositories.sample.response.GetResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.QueryMap


//contract
interface SampleService {
    suspend fun login(request : GetRequest) : GetResponse
    suspend fun logout()
    suspend fun register()
    suspend fun hasUser()
    suspend fun getCurrentUser()
    suspend fun sendEmailVerification(email : String)
    suspend fun sendPasswordReset(email : String)
}