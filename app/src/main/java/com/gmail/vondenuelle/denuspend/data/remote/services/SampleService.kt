package com.gmail.vondenuelle.denuspend.data.remote.services

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

//Contracts
interface SampleService {

    @GET("")
    suspend fun getRequest() : Response<GetResponse>

    @POST("")
    suspend fun postRequest(@Body request : GetRequest) : Response<GetResponse>

    @GET("{id}")
    suspend fun  getUrlParamsRequest(@Path("id") id : Int) : Response<GetResponse>

    @GET("")
    suspend fun getQueryParamsRequest(@QueryMap params : Map<String, String>): Response<GetResponse>

    @Multipart
    @POST("")
    suspend fun getMultiPartRequest(
        @Part frontImagePart: MultipartBody.Part,
        @Part backImagePart: MultipartBody.Part,
        @Part user_id: MultipartBody.Part
    ) : Response<GetResponse>
}