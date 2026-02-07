package com.gmail.vondenuelle.denuspend.domain.repositories.sample

import com.gmail.vondenuelle.denuspend.di.modules.IoDispatcher
import com.gmail.vondenuelle.denuspend.data.repositories.sample.SampleRepository
import com.gmail.vondenuelle.denuspend.data.repositories.sample.request.GetRequest
import com.gmail.vondenuelle.denuspend.data.repositories.sample.response.GetResponse
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

@ViewModelScoped
class SampleUseCase @Inject constructor(
    private val sampleRepository: SampleRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    fun getRequest(request: GetRequest) : Flow<GetResponse> {
        return flow {
            val response = sampleRepository.getRequest(request)
            emit(response)
        }.flowOn(ioDispatcher)
    }
}