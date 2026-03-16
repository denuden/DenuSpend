package com.gmail.vondenuelle.denuspend.ui.add.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavOptions
import com.gmail.vondenuelle.denuspend.R
import com.gmail.vondenuelle.denuspend.navigation.NavigationScreens
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenEvents
import com.gmail.vondenuelle.denuspend.ui.add.AddScreenState
import com.gmail.vondenuelle.denuspend.ui.add.AddViewModel
import com.gmail.vondenuelle.denuspend.ui.add.components.CategoryDropdownButton
import com.gmail.vondenuelle.denuspend.ui.common.components.InformationTooltip
import com.gmail.vondenuelle.denuspend.ui.profile.component.CurrencyTextField
import com.gmail.vondenuelle.denuspend.ui.theme.DenuSpendTheme

@Composable
fun AddExpenseScreen(
    onNavigate: (NavigationScreens, NavOptions?) -> Unit,
    onPopBackStack: () -> Unit,
    viewModel: AddViewModel = hiltViewModel()
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    AddExpenseScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun AddExpenseScreenContent(
    modifier: Modifier = Modifier,
    state: AddScreenState,
    onEvent: (AddScreenEvents) -> Unit
) {
    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        ElevatedCard(
            colors = CardDefaults.cardColors()
                .copy(containerColor = colorResource(R.color.not_verified_red_container)),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                InformationTooltip(
                    richTooltipSubheadText = "Adding Expense Transaction",
                    richTooltipText = "Add the amount of the expense transaction. You can input a title and description (E.G. Subscription for movies, From entertainment savings) ",
                    modifier = Modifier.padding(8.dp)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)
                ) {
                    Text("Enter Amount", style = MaterialTheme.typography.titleSmall)

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(contentAlignment = Alignment.Center) {
                        CurrencyTextField(
                            amountInCents = state.transactionAmount,
                            onAmountChange = {
                                onEvent(AddScreenEvents.OnIncomeAmountChanged(it))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp)
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            modifier = Modifier
                                .padding(top = 32.dp)
                                .fillMaxWidth(0.75f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        val outlinedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

        Spacer(modifier = Modifier.height(32.dp))


        Text(
            "Title (20 characters)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = state.transactionTitle,
            onValueChange = {
                if (it.length <= 20) {
                    onEvent(AddScreenEvents.OnIncomeTitleChanged(it))
                }
            },
            placeholder = { Text("Enter a title for this transaction") },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = outlinedColor,
                disabledIndicatorColor = outlinedColor,
                errorIndicatorColor = outlinedColor
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),

            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Description (50 characters)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = state.transactionDescription,
            onValueChange = {
                if (it.length <= 50) {
                    onEvent(AddScreenEvents.OnIncomeDescriptionChanged(it))
                }
            },
            placeholder = { Text("Enter a short description of your transaction") },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = outlinedColor,
                disabledIndicatorColor = outlinedColor,
                errorIndicatorColor = outlinedColor
            ),
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer( modifier = Modifier. height(16.dp))
        Text(
            "Income Category",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        CategoryDropdownButton(modifier = Modifier.fillMaxWidth())

        Spacer( modifier = Modifier. height(16.dp))
        FilledTonalButton(
            shape = MaterialTheme.shapes.extraLarge,
            onClick = {
                //TODO
            },
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Transaction")
        }
    }


}

@Preview
@Composable
private fun AddExpenseScreenPreview() {
    DenuSpendTheme() {
        Surface {
            AddExpenseScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = AddScreenState(),
                onEvent = {})
        }
    }
}