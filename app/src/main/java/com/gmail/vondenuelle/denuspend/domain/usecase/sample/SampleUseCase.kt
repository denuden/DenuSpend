package com.gmail.vondenuelle.denuspend.domain.usecase.sample

import com.gmail.vondenuelle.denuspend.di.qualifiers.IoDispatcher
import com.gmail.vondenuelle.denuspend.data.repositories.SampleRepository
import com.gmail.vondenuelle.denuspend.data.remote.models.sample.request.GetRequest
import com.gmail.vondenuelle.denuspend.data.remote.models.sample.response.GetResponse
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Only for merging data
 */
@ViewModelScoped
class SampleUseCase @Inject constructor(
    private val sampleRepository: SampleRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
}