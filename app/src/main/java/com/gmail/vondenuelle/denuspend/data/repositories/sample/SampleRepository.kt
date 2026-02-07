package com.gmail.vondenuelle.denuspend.data.repositories.sample

import com.gmail.vondenuelle.denuspend.data.remote.services.SampleService
import com.gmail.vondenuelle.denuspend.data.repositories.sample.request.GetRequest
import com.gmail.vondenuelle.denuspend.data.repositories.sample.response.GetResponse
import com.gmail.vondenuelle.denuspend.utils.network.ALL_IMAGE_MIME_TYPE
import com.gmail.vondenuelle.denuspend.utils.network.asNetWorkRequestBody
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.MultipartBody
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject

@ViewModelScoped
class SampleRepository @Inject constructor(
    private val sampleService: SampleService
) {
    suspend fun getRequest(request : GetRequest) : GetResponse {
        val map = mapOf(
            "id" to request.id
        ).filterValues { it != null }.mapValues { it.value.toString() }

        val response = sampleService.getRequest()

        if(response.code() != HttpURLConnection.HTTP_OK){
            throw HttpException(response)
        }

        return response.body() ?: throw NullPointerException("Response data is empty")
    }

    suspend fun getMultiPartRequest(request : GetRequest) : GetResponse {
        val response = sampleService.getMultiPartRequest(
            MultipartBody.Part.createFormData(
                "frontImagePart",
                request.frontImageFile?.name,
                request.frontImageFile.asNetWorkRequestBody(ALL_IMAGE_MIME_TYPE)
            ),
            MultipartBody.Part.createFormData(
                "backImagePart",
                request.backImageFile?.name,
                request.backImageFile.asNetWorkRequestBody(ALL_IMAGE_MIME_TYPE)
            ),
            MultipartBody.Part.createFormData("user_id", request.userId.orEmpty()),
        )

        if(response.code() != HttpURLConnection.HTTP_OK){
            throw HttpException(response)
        }

        return response.body() ?: throw NullPointerException("Response data is empty")
    }
}