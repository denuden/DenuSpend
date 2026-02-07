package com.gmail.vondenuelle.denuspend.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoroutineHelper(private var coroutineScope: CoroutineScope) {
    private var job: Job? = null

    fun debouncer(
        delayMs: Long = 500L,
        block: suspend () -> Unit) {
        job?.cancel()
        job = coroutineScope.launch {
            delay(delayMs)
            block()
        }
    }
}