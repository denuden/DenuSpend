package com.gmail.vondenuelle.denuspend.ui.budget.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.navigation.NavBehavior
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButtonModel
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenEvents
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetScreenState
import com.gmail.vondenuelle.denuspend.ui.budget.BudgetViewModel
import com.gmail.vondenuelle.denuspend.ui.budget.components.BudgetItem
import com.gmail.vondenuelle.denuspend.ui.budget.components.section.ChartSection
import com.gmail.vondenuelle.denuspend.ui.common.dialog.ErrorDialog
import com.gmail.vondenuelle.denuspend.ui.common.dialog.LoadingDialog
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme
import com.gmail.vondenuelle.denuspend.utils.ObserveAsEvents
import com.gmail.vondenuelle.denuspend.utils.OneTimeEvents
import com.gmail.vondenuelle.denuspend.utils.SnackBarController
import kotlinx.coroutines.launch

@Composable
fun BudgetScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.channel) { event ->
        when (event) {
            is OneTimeEvents.OnPopBackStack -> {
                onPopBackStack()
            }

            is OneTimeEvents.OnNavigate -> {
                val options = NavOptions.Builder().apply {
                    when (event.behavior) {
                        is NavBehavior.ClearAll -> {
                            setPopUpTo(0, inclusive = true)
                            setLaunchSingleTop(true)
                            setRestoreState(false)
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


    LoadingDialog(
        showDialog = state.isLoading,
        text = "Loading...",
    ) { }
    ErrorDialog(
        text = error,
        showDialog = error.isNotEmpty()
    ) { error = "" }

    BudgetScreenContent(
        state = state, onEvent = viewModel::onEvent,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreenContent(
    modifier: Modifier = Modifier,
    state: BudgetScreenState,
    onEvent: (BudgetScreenEvents) -> Unit,
    ) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val dropdownScrollState = rememberScrollState()
    var selectedFilter by remember { mutableStateOf<String>("This Month") }
    val list = remember {
        listOf(
            "This Month",
            "Last 7 Days",
            "Last 15 Days",
            "Last 30 Days",
            "Last 3 Months",
            "Last 6 Months",
            "This Year"
        )
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        ChartSection(
            expense = 254145600,
            budget = 456300000
        )

        Spacer(modifier = Modifier.height(16.dp))


        LaunchedEffect(expanded) {
            if (expanded) {
                // Scroll to show the bottom menu items.
                dropdownScrollState.scrollTo(dropdownScrollState.maxValue)
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            AssistChip(
                onClick = {
                    //Todo
                },
                label = {
                    Text(selectedFilter)
                },
                leadingIcon = {
                    Icon(Icons.Filled.CalendarMonth, null)
                },
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                scrollState = dropdownScrollState,
                matchAnchorWidth = false
            ) {
                list.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            expanded = false
                            selectedFilter = option
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        CategoryDropdownButtonModel.list.drop(1).mapIndexed { index, it ->
            BudgetItem(
                category = it,
                transactionCount = "5",
                onClick = {
                    onEvent(BudgetScreenEvents.OnNavigateToBudgetTransactionScreen(it))
                },
                icon = {
                    Icon(
                        imageVector = CategoryDropdownButtonModel.icons[index],
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun BudgetScreenContentPreview() {
    DenuSpendTheme {
        Surface {
            BudgetScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = BudgetScreenState(),
                onEvent =  {}
            )
        }
    }
}