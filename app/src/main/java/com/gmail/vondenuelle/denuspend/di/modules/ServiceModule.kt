package com.gmail.vondenuelle.denuspend.di.modules

import com.gmail.vondenuelle.denuspend.data.remote.services.auth.AuthService
import com.gmail.vondenuelle.denuspend.data.remote.services.auth.FirebaseAuthServiceImpl
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
    abstract fun bindAuthService(firebaseAuthServiceImpl: FirebaseAuthServiceImpl): AuthService
}