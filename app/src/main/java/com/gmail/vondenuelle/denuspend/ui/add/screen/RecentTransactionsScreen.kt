package com.gmail.vondenuelle.denuspend.ui.add.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.ui.common.components.PullToRefreshCustomStyle
import com.gmail.vondenuelle.denuspend.ui.common.components.TransactionItem
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.common.skeleton.SkeletonTransactionList
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ComposableLifecycle
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

@Composable
fun RecentTransactions(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack : () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }

    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> {
                onPopBackStack()
            }

            is OneTimeEvents.OnNavigate -> {
                val options = NavOptions.Builder().apply {
                    when (event.behavior) {
                        NavBehavior.ClearAll -> {
                            setPopUpTo(0, inclusive = true)
                            setLaunchSingleTop(true)
                        }

                        is NavBehavior.PopUpTo -> {
                            setPopUpTo(
                                event.behavior.destination,
                                inclusive = event.behavior.inclusive
                            )
                        }

                        NavBehavior.None -> Unit
                    }
                }.build()

                onNavigate(event.route, options)
            }

            is OneTimeEvents.ShowError -> error = event.msg
            is OneTimeEvents.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT)
                .show()

            is OneTimeEvents.ShowSnackbar -> {
                scope.launch {
                    SnackBarController.sendEvent(event.snackbarEvent)
                }
            }

            OneTimeEvents.OnCloseDialog -> {}
            is OneTimeEvents.ShowInputError -> {}
        }
    }

    ComposableLifecycle(lifecycle) { _, event ->
        if(event == Lifecycle.Event.ON_START ) {
            viewModel.setTransactionLimit(null)
        }
    }

    LoadingDialog(
        showDialog = state.isLoading,
        text = "Loading...",
    ) { }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }
    RecentTransactionsContent(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun RecentTransactionsContent(
    modifier: Modifier = Modifier,
    onEvent: (AddScreenEvents) -> Unit,
    state: AddScreenState
) {
    PullToRefreshCustomStyle(
        isRefreshing = state.isListLoading,
        onRefresh =  {
            onEvent(AddScreenEvents.OnGetAllTransactions)
        }
    ) {
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = state.isListLoading
        ) {
            Column(
                modifier = Modifier
            ) {
                repeat(5) {
                    SkeletonTransactionList(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !state.isListLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ){
            LazyColumn(
                modifier = modifier
                    .padding(16.dp)
            ){
                items(state.transactionList){
                    TransactionItem(transactionModel = it)
                }

                if(state.transactionList.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Nothing to show", fontWeight = FontWeight.W300)
                        }
                    }
                }
            }
        }

    }
}

@Preview
@Composable
private fun RecentTransactionsPreview() {
    DenuSpendTheme {
        Surface {
            RecentTransactionsContent(
                modifier = Modifier.fillMaxSize(),
                state = AddScreenState(),
                onEvent = {}
            )
        }
    }
}