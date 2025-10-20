package com.gmail.denuelle42.denuspend.di.modules

import javax.inject.Qualifier

//https://medium.com/androiddevelopers/create-an-application-coroutinescope-using-hilt-dd444e721528
/**
 *  Since all the dispatchers are of the same type (CoroutineDispatcher),
 *  simply providing them without qualifiers would confuse Hilt—it wouldn’t know which one to inject where.
 *  By creating custom qualifiers (like @IoDispatcher or @MainDispatcher), you’re giving Hilt a way to distinguish between them,
 *  so it knows exactly which dispatcher to inject in each case.
 */

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainImmediateDispatcher