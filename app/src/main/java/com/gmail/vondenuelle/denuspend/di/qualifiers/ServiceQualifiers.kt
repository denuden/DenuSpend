package com.gmail.vondenuelle.denuspend.di.qualifiers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FirebaseProfile

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FirebaseAuth

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FirebaseTransaction

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FirebaseSample

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RetrofitSample