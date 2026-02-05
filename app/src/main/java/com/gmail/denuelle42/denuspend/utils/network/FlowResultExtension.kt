package com.gmail.denuelle42.denuspend.utils.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.io.IOException


/**
 * Credits to: Link -> https://www.valueof.io/blog/stateflow-sharedflow-flow-viewmodel-lifecycle
 * This wraps results in Success, Error, and automatic Loading, Completed states, onStart()
 */
private const val RETRY_TIME_IN_MILLIS = 15_000L
private const val RETRY_ATTEMPT_COUNT = 3

sealed interface ResultState<out T> {
    data class Success<T>(val data: T) : ResultState<T>
    data class Error(val exception: Throwable? = null, val message : String = "") :
        ResultState<Nothing>
    object Loading : ResultState<Nothing>
    object Completed : ResultState<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<ResultState<T>> {
    return this
        .map<T, ResultState<T>> {
            ResultState.Success(it)
        }
        .onStart { emit(ResultState.Loading) }
        .retryWhen { cause, attempt ->
            if (cause is IOException && attempt < RETRY_ATTEMPT_COUNT) {
                delay(RETRY_TIME_IN_MILLIS)
                true
            } else {
                false
            }
        }
        .catch { emit(ResultState.Error(it)) }
        .onCompletion { emit(ResultState.Completed) }
}