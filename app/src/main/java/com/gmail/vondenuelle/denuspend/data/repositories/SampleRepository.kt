package com.gmail.vondenuelle.denuspend.data.repositories

import com.gmail.vondenuelle.denuspend.data.remote.services.sample.SampleService
import com.gmail.vondenuelle.denuspend.data.remote.models.sample.request.GetRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.sample.response.GetResponse
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseSample
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class SampleRepository @Inject constructor(
   @FirebaseSample private val service : SampleService
) {
    suspend fun login(request: GetRequest) : GetResponse {
        return service.login(request)
    }
}