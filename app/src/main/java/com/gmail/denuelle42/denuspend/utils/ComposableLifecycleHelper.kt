package com.gmail.denuelle42.denuspend.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


/**
 * https://www.youtube.com/watch?v=KFazs62lIkE
 *
 * Observe a flow as events, making it lifecycle aware
 * where it will trigger and reset everytime lifecycle state changes
 * While also being able to pass keys as potential triggers
 *
 * Making flow to only be collected from when LifeCycle is STARTED
 * and running on main thread as immediate dispatcher
 *
 * uses:
 *  // observers snackbar controller events flow globally
 *     // snackbarhoststate as key to trigger launchedeffect block
 *     // SnackBarController.events is the flow being observed and collected
 *     ObserveAsEvents(flow = SnackBarController.events, snackbarHostState) { event ->
 *         scope.launch {
 *             snackbarHostState.currentSnackbarData?.dismiss() //dismiss ongoing snackbar
 *             val result = snackbarHostState.showSnackbar( // launch new snackbar
 *                 message = event.message, actionLabel = event.action?.name,
 *                 duration = SnackbarDuration.Long
 *             )
 *
 *             if (result == SnackbarResult.ActionPerformed) {
 *                 event.action?.action?.invoke()
 *             }
 *         }
 *     }
 */
@Composable
fun <T>ObserveAsEvents(
    flow: Flow<T>,
    key1: Any? = null,
    key2: Any? = null,
    onEvent : (T) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner.lifecycle, key1 ,key2, flow) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
            withContext(Dispatchers.Main.immediate){
                flow.collect(onEvent)
            }
        }
    }
}


/**
 * https://stackoverflow.com/questions/71239101/how-to-listen-for-lifecycle-in-jetpack-compose
 * Makes an observer that observes lifecycle states and trigger events based on it
 * Automatically disposes that event and observer when lifecycle changes
 *
 * uses:
 *    ComposableLifecycle { source, event ->
 *         if (event == Lifecycle.Event.ON_RESUME) {
 *             viewModel.onEvent(HomeScreenEvents.GetAllArticles)
 *         }
 *     }
 */
@Composable
fun ComposableLifecycle(
    lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onEvent: (LifecycleOwner, Lifecycle.Event) -> Unit
) {
    DisposableEffect(lifeCycleOwner) {
        val observer = LifecycleEventObserver { source, event ->
            onEvent(source, event)
        }
        lifeCycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifeCycleOwner.lifecycle.removeObserver(observer)
        }
    }
}