package com.gmail.vondenuelle.denuspend.di.modules

import com.gmail.vondenuelle.denuspend.data.remote.services.auth.AuthService
import com.gmail.vondenuelle.denuspend.data.remote.services.auth.FirebaseAuthServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.budget.BudgetService
import com.gmail.vondenuelle.denuspend.data.remote.services.budget.FirebaseBudgetServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.profile.FirebaseProfileServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.profile.ProfileService
import com.gmail.vondenuelle.denuspend.data.remote.services.sample.FirebaseSampleServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.sample.RetrofitSampleServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.sample.SampleService
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.FirebaseTransactionServiceImpl
import com.gmail.vondenuelle.denuspend.data.remote.services.transaction.TransactionService
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseAuth
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseBudget
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseProfile
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseSample
import com.gmail.vondenuelle.denuspend.di.qualifiers.FirebaseTransaction
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
    @FirebaseProfile
    abstract fun bindProfileService(firebaseProfileServiceImpl: FirebaseProfileServiceImpl): ProfileService

    @Binds
    @FirebaseAuth
    abstract fun bindAuthService(firebaseAuthServiceImpl: FirebaseAuthServiceImpl): AuthService

    @Binds
    @FirebaseTransaction
    abstract fun bindTransactionService(firebaseTransactionServiceImpl: FirebaseTransactionServiceImpl): TransactionService


    @Binds
    @FirebaseBudget
    abstract fun bindBudgetService(firebaseBudgetServiceImpl: FirebaseBudgetServiceImpl): BudgetService




    //select either retrofit impl or firebase impl
    @Binds
    @FirebaseSample
    abstract fun bindSampleServiceToFirebase(sampleServiceImpl: FirebaseSampleServiceImpl): SampleService

    @Binds
    @RetrofitSample
    abstract fun bindSampleServiceToRetrofit(sampleServiceImpl: RetrofitSampleServiceImpl): SampleService
}