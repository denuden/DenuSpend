package com.gmail.vondenuelle.denuspend.di.modules

import com.gmail.vondenuelle.denuspend.data.remote.services.auth.AuthService
import com.gmail.vondenuelle.denuspend.data.remote.services.auth.FirebaseAuthServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.sample.FirebaseSampleServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.sample.RetrofitSampleServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.sample.SampleService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseAuth
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseSample
import com.gmail.vondenuelle.denuspend.di.qualifiers.RetrofitSample
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    //it does not create the instance itself, it just tells Hilt:
    //“Use this implementation whenever you need the interface or the one that its returning”
    //It’s essentially a mapping between an interface (contract) and its implementation.

    @Binds
    @FirebaseAuth
    abstract fun bindAuthService(firebaseAuthServiceImpl: FirebaseAuthServiceImpl): AuthService


    //select either retrofit impl or firebase impl
    @Binds
    @FirebaseSample
    abstract fun bindSampleServiceToFirebase(sampleServiceImpl: FirebaseSampleServiceImpl): SampleService

    @Binds
    @RetrofitSample
    abstract fun bindSampleServiceToRetrofit(sampleServiceImpl: RetrofitSampleServiceImpl): SampleService
}