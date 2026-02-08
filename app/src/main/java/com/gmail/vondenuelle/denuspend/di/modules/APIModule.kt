package com.gmail.vondenuelle.denuspend.di.modules

import com.gmail.vondenuelle.denuspend.data.remote.services.sample.SampleAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object APIModule {
    @Provides
    @Singleton
    fun provideSampleAPI(retrofit: Retrofit): SampleAPI {
        return retrofit.create(SampleAPI::class.java)
    }
}